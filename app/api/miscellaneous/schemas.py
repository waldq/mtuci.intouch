from pydantic import BaseModel, Field
from typing import Annotated

#Модель для получения расписания.
class TimetableIn(BaseModel):
    group: str
    month: Annotated[int, Field(
        ge=1, le=6)] | Annotated[int, Field(ge=9, le=12)]
