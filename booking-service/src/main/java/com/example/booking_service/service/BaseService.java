package com.example.booking_service.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.exception.BadRequestException;

public class BaseService<E, ID> {

    private final JpaRepository<E, ID> repository;

    public BaseService(JpaRepository<E, ID> repository) {
        this.repository = repository;
    }

    public E create(E entity) {
        return repository.save(entity);
    }

    public E update(E entity) {
        return repository.save(entity);
    }

    @Transactional
    public List<E> updateAll(List<E> entities) {
        if (entities == null || entities.isEmpty() || entities.stream().anyMatch(entity -> entity == null)) {
            throw new BadRequestException("Update list must not be empty or contain null items");
        }
        return repository.saveAll(entities);
    }

    public List<E> findAll() {
        return repository.findAll();
    }

    public E findById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found with id: " + id));
    }

    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    @Transactional
    public void deleteById(ID id) {
        E entity = findById(id);
        repository.delete(entity);
    }

}
