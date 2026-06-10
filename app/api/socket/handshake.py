import redis.asyncio as redis
from sqlalchemy.ext.asyncio import AsyncSession
import json
import secrets

from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers import aead
from cryptography.hazmat.primitives import hashes, hmac
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat
from cryptography.hazmat.primitives.constant_time import bytes_eq
from cryptography.hazmat.backends.openssl import backend


from app.api.auth.crud import update_user_private_key
from app.api.socket.server import sio
from app.api.socket.schemas import (FHMQVStep1In, 
                                    FHMQVStep1Out,
                                    FHMQVStep2In,
                                    FHMQVStep2Encrypt,
                                    FHMQVStep2Out,
                                    FHMQVStep3In
                                    )
from app.redis_client import (RedisClient, 
                              store_client_public_static_key,
                              store_client_public_ephem_key, 
                              store_server_private_static_key,
                              store_server_public_static_key,
                              store_server_private_ephem_key,
                              store_server_public_ephem_key,
                              store_handshake_first_key,
                              store_k1_key,
                              store_sigma_b_bytes_hex)
from app.core.config import settings
from app.db.database import get_session
from app.api.users.crud import update_user_pub_key

 # Первый шаг рукопожатия во время регистрации. 
 # Клиент (Алиса) отправляет свой публичный постоянный ключ на сервер.
 # Сервер удостоверяется, что точка принадлежит кривой SECP256R1, что служит началом протокола.
@sio.on('fhmqv_step_1_start')
async def step_1_start_handler(sid,
                         data: FHMQVStep1In,
                         ):
    if not isinstance(data, FHMQVStep1In):
        await sio.emit('failed_handshake', {'result': 'Failure.'}, to=sid)
    client_public_static_key_bytes = bytes.fromhex(data.client_public_static_key_bytes_hex)
    try:
        redis_client = await RedisClient.get_client()
        
        try: # Сервер проверяет, что точка принадлежит кривой SECP256R1. В противном случае возвращает ошибку.
            client_public_static_key = ec.EllipticCurvePublicKey.from_encoded_point(ec.SECP256R1, data=client_public_static_key_bytes)
        except Exception as e:
            raise ValueError('Неверный формат ключа') from e
        # Сервер сохраняет публичный ключ в Redis на 30 секунд. В нормальном сценарии этого времени более чем достаточно для рукопожатия.
        await store_client_public_static_key(redis_client, sid, data.client_public_static_key_bytes_hex)

        # Сервер выбирает случайным образом случайный приватный ключ и считает для него публичный.
        # Сохраняет оба ключа в Redis
        random_raw_server_private_static_kay = settings.get_random_private_key()

        await store_server_private_static_key(
            redis_client=redis_client,
            session_id=sid,
            server_private_static_key_hex=random_raw_server_private_static_kay
        )

        raw_server_private_static_key = int(random_raw_server_private_static_kay, 16) 
        server_private_static_key = ec.derive_private_key( # Приватный статичный ключ сервера.
            private_value=raw_server_private_static_key, 
            curve=ec.SECP256R1())
        server_public_static_key = server_private_static_key.public_key() # Публичный статичный ключ сервера.

        # Сервер переводит публичный постоянные ключи сервера и клиента в байты для дальнейших операций.
        server_public_static_key_bytes = server_public_static_key.public_bytes(
                encoding=Encoding.X962, 
                format=PublicFormat.UncompressedPoint
                )
        
        client_public_static_key_bytes = client_public_static_key.public_bytes(
                encoding=Encoding.X962, 
                format=PublicFormat.UncompressedPoint
                )
        
        # Сервер хэширует свой публичный ключ для последующей отправки клиенту, поскольку у клиента заранее зашиты публичные ключи сервера.
        server_public_static_key_hash_digest = hashes.Hash(hashes.SHA3_256())
        server_public_static_key_hash_digest.update(
            server_public_static_key_bytes
            )
        server_public_static_key_hash_bytes = server_public_static_key_hash_digest.finalize()
        
        # Сервер сохраняет свой публичный постоянный ключ в Redis на время рукопожатия.
        await store_server_public_static_key(
            redis_client=redis_client, 
            session_id=sid, 
            server_public_static_key_bytes_hex=server_public_static_key_bytes.hex())
        
        # Сервер считает общий ключ по алгоритму Диффи-Хеллмана на эллиптических кривых (ECDH). 
        # Он нужен для симметричного AES шифрования на время рукопожатия. В дальнейшем этот ключ нигде применяться не будет.
        shared_first_key_bytes = server_private_static_key.exchange(algorithm=ec.ECDH(), peer_public_key=client_public_static_key)
        shared_first_derived_key_bytes = HKDF(
            algorithm=hashes.SHA3_256(),
            length=32,
            salt=None,
            info=None
        ).derive(key_material=shared_first_key_bytes)
        
        # Сервер высчитывает хэш публичных постоянных ключей клиента и сервера, а также общего ключа ECDH.
        handshake_start_key_digest = hashes.Hash(hashes.SHA3_256())
        handshake_start_key_digest.update(client_public_static_key_bytes)
        handshake_start_key_digest.update(server_public_static_key_bytes)
        handshake_start_key_digest.update(shared_first_derived_key_bytes)
        handshake_start_key_bytes = handshake_start_key_digest.finalize()

        # Сервер сохраняет ключ для рукопожатия в Redis.
        await store_handshake_first_key(
            redis_client=redis_client,
            session_id=sid,
            handshake_first_key_bytes_hex=handshake_start_key_bytes.hex())
        
        # Сервер создаёт модель для отправки клиенту. 
        response_data = FHMQVStep1Out(
            server_public_static_key_hash_bytes_hex=server_public_static_key_hash_bytes.hex()
            )

        # Сервер получает id клиента из сессии sio, поскольку в каждом событии connect сервер сохраняет id пользователя.
        user_data = sio.get_session(sid)
        user_id = user_data.get('user_id')

        # Сервер сохраняет постоянный публичный ключ пользователя в базу данных, поскольку этот ключ будет меняться только при повторном входе.
        # В дальнейшем сервер в качестве постоянного публичного ключа пользователя будет использовать только его.
        async with get_session() as session:
            await update_user_pub_key(user_id, client_public_static_key_bytes.hex(), session)

        # Сервер вызывает событие для продолжения рукопожатия, отправляя клиенту хэш своего публичного ключа.
        await sio.emit(event='fhmqv_step1_end', data=response_data.model_dump(), to=sid)
        
    except:
        raise ValueError('Incorrect key.')
    

# Второй этап рукопожатия, который инициализируется сразу после вычислений клиента.
# Клиент отправляет на сервер хэши постоянных публичных ключей клиента и сервера, свой публичный эфемерный ключ
# и те же саммые данные, но зашифрованные с помощью AESGCM. Помимо этого клиент отправляет nonce (96-битное число),
# использованное для шифрования. В качестве assiciated_data используется sid, общий как для клиента, так и для сервера.
@sio.on('fhmqv_step2_start')
async def step_2_start_handler(sid, data: FHMQVStep2In):
    if not isinstance(data, FHMQVStep2In):
        await sio.emit('failed_handshake', {'result': 'Failure.'}, to=sid)
    try:
        redis_client = await RedisClient.get_client()

        # Сервер получает ключ рукопожатия, публичные постоянные ключи сервера и клиента из Redis.
        # В случае их отсутствия сессия считается истёкшей, т.к прошло более 30 секунд. В этом случае 
        # В этом случае соединения считается небезопасным, потому что в обычном сценарии этого времени
        # должно быть более чем достаточно для рукопожатия.
        handshake_start_key_bytes_hex = await redis_client.get(f'handshake:start:key:{sid}')

        server_private_static_key_bytes_hex = await redis_client.get(f'handshake:server:private:static:{sid}')
        server_public_static_key_bytes_hex = await redis_client.get(f'handshake:server:public:static:{sid}')

        client_public_static_key_bytes_hex = await redis_client.get(f'handshake:client:public:static:{sid}')
        
        if not all([
                handshake_start_key_bytes_hex, 
                server_private_static_key_bytes_hex,
                server_public_static_key_bytes_hex, 
                client_public_static_key_bytes_hex
                ]):
            raise ValueError('Сессия истекла. Рукопожатие длилось дольше 30 секунд, есть вероятность незащищённого соединения.')

        # Сервер переводит получаенные из Redis ключи в байты.
        handshake_start_key_bytes = bytes.fromhex(handshake_start_key_bytes_hex)
        server_public_static_key_bytes = bytes.fromhex(server_public_static_key_bytes_hex)
        client_public_static_key_bytes = bytes.fromhex(client_public_static_key_bytes_hex)

        # Сервер хэширует публичные постоянные ключи клиента и сервера для дальнейшей проверки 
        # полученных  от клиента данных на этом этапе рукопожатия. 
        server_public_static_key_hash_digest = hashes.Hash(hashes.SHA3_256())
        server_public_static_key_hash_digest.update(server_public_static_key_bytes)
        server_public_static_key_hash_bytes = server_public_static_key_hash_digest.finalize()

        client_public_static_key_hash_digest = hashes.Hash(hashes.SHA3_256())
        client_public_static_key_hash_digest.update(client_public_static_key_bytes)
        client_public_static_key_hash_bytes = client_public_static_key_hash_digest.finalize()

        # В случае неравенства хэшей, вычисленных сервером, и хэшей, отправленных клиентов,
        # выводится ошибка и рукопожатие прекращается. 
        if not all([
            bytes_eq(server_public_static_key_hash_bytes, bytes.fromhex(data.server_public_static_key_hash_bytes_hex)),
            bytes_eq(client_public_static_key_hash_bytes, bytes.fromhex(data.client_public_static_key_hash_bytes_hex))
            ]):
            raise ValueError('Ключи не совпадают. Есть вероятность незащищённого соединения.')

        # Создаётся объект для шифрования и расшифрования по AESGCM с использованием общего
        # ключа, получаенного на этапе 1.
        aesgcm = aead.AESGCM(handshake_start_key_bytes)

        # nonce берётся из полученных от клиента данных, в качестве associated_data берётся sid.
        nonce = bytes.fromhex(data.nonce)
        associated_data = sid

        # Полученные данные переводятся в байты для дальнейшего расшифрования.
        aes_encrypted_keys_data_bytes = bytes.fromhex(data.aes_encrypted_keys_data_bytes_hex)
        encrypted_keys_data_bytes = aesgcm.decrypt(nonce=nonce, data=aes_encrypted_keys_data_bytes, associated_data=associated_data)

        # Расшифрованные данные переводятся в dict для дальнейшего извлечения значений оттуда.
        # Эти данные сверяются с данными, отправленными не зашифрованно. В случае расхождений
        # рукопожатие прекращается.
        decoded_encrypted_data_string = encrypted_keys_data_bytes.decode('utf-8')
        decoded_encrypted_data_dict = json.loads(decoded_encrypted_data_string)

        encrypted_client_public_static_key_hash_bytes_hex = decoded_encrypted_data_dict.get('client_public_static_key_hash_bytes_hex')
        deciphered_client_public_static_key_hash_bytes = bytes.fromhex(encrypted_client_public_static_key_hash_bytes_hex)

        encrypted_server_public_static_key_hash_bytes_hex = decoded_encrypted_data_dict.get('server_public_static_key_hash_bytes_hex')
        deciphered_server_public_static_key_hash_bytes = bytes.fromhex(encrypted_server_public_static_key_hash_bytes_hex)

        encrypted_client_public_ephem_key_bytes_hex = decoded_encrypted_data_dict.get('client_public_ephem_key_bytes_hex')
        deciphered_client_public_ephem_key_bytes = bytes.fromhex(encrypted_client_public_ephem_key_bytes_hex)

        client_public_ephem_key_bytes_hex = data.client_public_ephem_key_bytes_hex
        client_public_ephem_key_bytes = bytes.fromhex(client_public_ephem_key_bytes_hex)

        if not all([
            bytes_eq(deciphered_client_public_static_key_hash_bytes, client_public_static_key_hash_bytes),
            bytes_eq(deciphered_server_public_static_key_hash_bytes, server_public_static_key_hash_bytes),
            bytes_eq(deciphered_client_public_ephem_key_bytes, client_public_ephem_key_bytes)
        ]):
            raise ValueError('Шифрованные и нешифрованные ключи не совпадают.')
        
        # Сервер удостоверяется, что присланная клиентом точка принадлежит кривой.
        client_public_ephem_key = ec.EllipticCurvePublicKey.from_encoded_point(curve=ec.SECP256R1, data=client_public_ephem_key_bytes)

        # Сервер сохраняет публичный эфемерный ключ клиента в Redis.
        await store_client_public_ephem_key(
            redis_client=redis_client,
            session_id=sid,
            client_public_ephem_key_bytes_hex=client_public_ephem_key_bytes_hex)

        # Сервер генерирует свой приватный эфемерный ключ,
        # сохраняет этот ключ в виде числа в Redis, а также 
        # считает для него публичный эфемерный ключ.
        server_private_ephem_key = ec.generate_private_key(ec.SECP256R1)

        raw_server_private_ephem_key_number = server_private_ephem_key.private_numbers().private_value

        await store_server_private_ephem_key(
            redis_client=redis_client,
            session_id=sid,
            server_private_ephem_key=raw_server_private_ephem_key_number.to_bytes().hex()
            )

        server_public_ephem_key = server_private_ephem_key.public_key()
        server_public_ephem_key_bytes = server_public_ephem_key.public_bytes(
            encoding=Encoding.X962, 
            format=PublicFormat.UncompressedPoint
        )

        await store_server_public_ephem_key(
            redis_client=redis_client,
            session_id=sid,
            server_public_ephem_key_bytes_hex=server_public_ephem_key_bytes.hex()
        )


        d_e_digest = hashes.Hash(hashes.SHA3_256())
        d_e_digest.update(client_public_ephem_key_bytes)
        d_e_digest.update(server_public_ephem_key_bytes)
        d_e_digest.update(client_public_static_key_bytes)
        d_e_digest.update(server_public_static_key_bytes)
        d_e = d_e_digest.finalize()
        
        d = int.from_bytes(d_e[:16], 'big')
        e = int.from_bytes(d_e[16:], 'big')

        y = raw_server_private_ephem_key_number
        b = int(server_private_static_key_bytes_hex, 16)

        group_order = int(0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551, 16)

        bn_order = backend._lib.BN_new()
        bn_y = backend._lib.BN_new()
        bn_e = backend._lib.BN_new()
        bn_b = backend._lib.BN_new()
        bn_d = backend._lib.BN_new()

        bn_eb = backend._lib.BN_new()
        bn_s_B = backend._lib.BN_new()
        bn_k1 = backend._lib.BN_new()
        bn_k2 = backend._lib.BN_new()

        bn_ctx = backend._lib.BN_CTX_new()

        try:
            for val, bn_obj in [(group_order, bn_order), (y, bn_y), (e, bn_e), (b, bn_b), (d, bn_d)]:
                val_bytes = val.to_bytes(length=32, byteorder='big')
                backend._lib.BN_bin2bn(val_bytes, len(val_bytes), bn_obj)
            backend._lib.BN_mod_mul(bn_eb, bn_e, bn_b, bn_order, bn_ctx)

            backend._lib.BN_mod_add(bn_s_B, bn_y, bn_eb, bn_order, bn_ctx)

            backend._lib.BN_copy(bn_k1, bn_s_B)

            backend._lib.BN_mod_mul(bn_k2, bn_s_B, bn_d, bn_order, bn_ctx)

            NID_X9_62_prime256v1 = 415
            group = backend._lib.EC_GROUP_new_by_curve_name(NID_X9_62_prime256v1)

            pt_X = backend._lib.EC_POINT_new(group)
            pt_A = backend._lib.EC_POINT_new(group)
            comp1 = backend._lib.EC_POINT_new(group)
            comp2 = backend._lib.EC_POINT_new(group)
            sigma_point = backend._lib.EC_POINT_new(group)
            try:
                backend._lib.EC_POINT_oct2point(group, pt_X, client_public_ephem_key_bytes, len(client_public_ephem_key_bytes), bn_ctx)
                backend._lib.EC_POINT_oct2point(group, pt_A, client_public_static_key_bytes, len(client_public_static_key_bytes), bn_ctx)

                backend._lib.EC_POINT_mul(group, comp1, backend._ffi.NULL, pt_X, bn_k1, bn_ctx)
                backend._lib.EC_POINT_mul(group, comp2, backend._ffi.NULL, pt_A, bn_k2, bn_ctx)

                backend._lib.EC_POINT_add(group, sigma_point, comp1, comp2, bn_ctx)


                if backend._lib.EC_POINT_is_at_infinity(group, sigma_point) == 1:
                    raise ValueError("Вычисленная точка общего секрета находится в бесконечности.")
                
                bn_x_coord = backend._lib.BN_new()
                try:
                    backend._lib.EC_POINT_get_affine_coordinates(group, sigma_point, bn_x_coord, backend._ffi.NULL, bn_ctx)

                    num_bytes = backend._lib.BN_num_bytes(bn_x_coord)
                    buf = backend._ffi.new("unsigned char[]", num_bytes)
                    backend._lib.BN_bn2bin(bn_x_coord, buf)
                    sigma_b_bytes = backend._ffi.buffer(buf, num_bytes)[:]
                finally:
                    backend._lib.BN_clear_free(bn_x_coord)
            
            finally:
                backend._lib.EC_POINT_free(pt_X)
                backend._lib.EC_POINT_free(pt_A)
                backend._lib.EC_POINT_free(comp1)
                backend._lib.EC_POINT_free(comp2)
                backend._lib.EC_POINT_free(sigma_point)
                backend._lib.EC_GROUP_free(group)

        finally:
            backend._lib.BN_clear_free(bn_y)
            backend._lib.BN_clear_free(bn_e)
            backend._lib.BN_clear_free(bn_b)
            backend._lib.BN_clear_free(bn_d)
            backend._lib.BN_clear_free(bn_eb)
            backend._lib.BN_clear_free(bn_s_B)
            backend._lib.BN_clear_free(bn_k1)
            backend._lib.BN_clear_free(bn_k2)
            backend._lib.BN_clear_free(bn_order)
            backend._lib.BN_CTX_free(bn_ctx)
        
        await store_sigma_b_bytes_hex(sigma_b_bytes.hex())

        kdf_context = (
            client_public_static_key_bytes +
            server_public_static_key_bytes +
            client_public_ephem_key_bytes +
            server_public_ephem_key_bytes
        )

        k1_key_bytes = HKDF(
            algorithm=hashes.SHA3_256(),
            length=32,
            salt=None,
            info=kdf_context
        ).derive(sigma_b_bytes)

        await store_k1_key(
            redis_client,
            sid,
            k1_key_bytes.hex()
        )

        mac_context = server_public_static_key_bytes + server_public_ephem_key_bytes
        t_b_bytes_digest = hmac.HMAC(k1_key_bytes, hashes.SHA3_256())
        t_b_bytes_digest.update(mac_context)
        t_b_bytes = t_b_bytes_digest.finalize()

        server_nonce = secrets.randbits(96).to_bytes()

        aes_payload = FHMQVStep2Encrypt(
            server_public_static_key_hash_bytes_hex=server_public_static_key_hash_bytes.hex(),
            client_public_static_key_hash_bytes_hex=client_public_static_key_hash_bytes.hex(),
            server_public_ephem_key_bytes_hex=server_public_ephem_key_bytes.hex(),
            t_b_bytes_hex=t_b_bytes.hex()
        ).model_dump()
        aes_payload_dumped = json.dumps(aes_payload).encode('utf-8')

        aes_encrypted_keys_and_tb_data_bytes = aesgcm.encrypt(
            server_nonce,
            aes_payload_dumped,
            sid
        )

        response_model = FHMQVStep2Out(
            server_public_static_key_hash_bytes.hex(),
            client_public_static_key_hash_bytes.hex(),
            server_public_ephem_key_bytes.hex(),
            t_b_bytes.hex(),
            aes_encrypted_keys_and_tb_data_bytes.hex(),
            server_nonce
            ).model_dump()

        sio.emit('fhmqv_step2_end', response_model, to=sid)

    except:
        raise ValueError('Что-то пошло не так.')
    
    
@sio.on('fhmqv_step3_start')
async def step_3_start_handler(sid, data: FHMQVStep3In):
    if not isinstance(data, FHMQVStep3In):
        await sio.emit('failed_handshake', {'result': 'Failure.'}, to=sid)
    try:
        user_data = sio.get_session(sid)
        user_id = user_data.get('user_id')

        redis_client = await RedisClient.get_client()

        handshake_start_key_hex = await redis_client.get(f'handshake:start:key:{sid}')

        client_public_static_key_bytes_hex = await redis_client.get(f'handshake:client:public:static:key:{sid}')
        client_public_ephem_key_bytes_hex = await redis_client.get(f'handshake:client:public:ephem:key:{sid}')

        server_public_static_key_bytes_hex = await redis_client.get(f'handshake:server:public:static:{sid}')
        server_public_ephem_key_bytes_hex = await redis_client.get(f'handshake:server:public:ephem:{sid}')

        k1_key_bytes_hex =await redis_client.get(f'handshake:k1:{sid}')
        sigma_b_bytes_hex = await redis_client.get(f'handshake:sigmab:{sid}')

        if not all([
            handshake_start_key_hex,
            client_public_static_key_bytes_hex,
            client_public_ephem_key_bytes_hex,
            server_public_static_key_bytes_hex,
            server_public_ephem_key_bytes_hex,
            k1_key_bytes_hex,
            sigma_b_bytes_hex
        ]):
            raise ValueError('Сессия истекла. Рукопожатие длилось дольше 30 секунд, есть вероятность незащищённого соединения.')

        handshake_start_key = bytes.fromhex(handshake_start_key_hex)

        client_public_static_key_bytes = bytes.fromhex(client_public_static_key_bytes_hex)
        client_public_ephem_key_bytes = bytes.fromhex(client_public_ephem_key_bytes_hex)

        server_public_static_key_bytes = bytes.fromhex(client_public_static_key_bytes_hex)
        server_public_ephem_key_bytes = bytes.fromhex(client_public_ephem_key_bytes_hex)

        k1_key_bytes = bytes.fromhex(k1_key_bytes_hex)
        sigma_b_bytes = bytes.fromhex(sigma_b_bytes_hex)

        mac_context = client_public_static_key_bytes + client_public_ephem_key_bytes
        t_a_server_bytes_digest = hmac.HMAC(k1_key_bytes, hashes.SHA3_256())
        t_a_server_bytes_digest.update(mac_context)
        t_a_server_bytes = t_a_server_bytes_digest.finalize()

        aesgcm = aead.AESGCM(handshake_start_key)

        nonce = bytes.fromhex(data.nonce)
        associated_data = sid

        aes_encrypted_ta_bytes = bytes.fromhex(data.aes_encrypted_ta_bytes_hex)
        encrypted_ta_bytes = aesgcm.decrypt(nonce=nonce, data=aes_encrypted_ta_bytes, associated_data=associated_data)

        decoded_encrypted_data_string = encrypted_ta_bytes.decode('utf-8')
        decoded_encrypted_data_dict = json.loads(decoded_encrypted_data_string)

        encrypted_t_a_bytes_hex = decoded_encrypted_data_dict.get('t_a_bytes_hex')
        deciphered_t_a_bytes_hex = bytes.fromhex(encrypted_t_a_bytes_hex)

        if not (bytes_eq(t_a_server_bytes, deciphered_t_a_bytes_hex),
                bytes_eq(deciphered_t_a_bytes_hex, data.t_a_bytes_hex)):
            raise ValueError('Данные не совпадают. Есть вероятность незащищённого соединения.')

        kdf_context = (
            sigma_b_bytes,
            k1_key_bytes,
            client_public_static_key_bytes +
            server_public_static_key_bytes +
            client_public_ephem_key_bytes +
            server_public_ephem_key_bytes
        )

        k2_key_bytes = HKDF(
            algorithm=hashes.SHA3_256(),
            length=32,
            salt=None,
            info=kdf_context
        ).derive(k1_key_bytes)


        result = await update_user_private_key(user_id, k2_key_bytes)
        if result.get('result') == 'Success.':
            sio.emit('fhmqv_end', {'result': 'Success.'}, to=sid)

    except:
        raise ValueError('Что-то пошло не так.')