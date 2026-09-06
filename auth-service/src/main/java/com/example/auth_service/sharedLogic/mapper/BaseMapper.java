package com.example.auth_service.sharedLogic.mapper;

public abstract class BaseMapper<E, D> {

    public abstract E toEntity(D dto);

    public abstract D toDto(E entity);
}
