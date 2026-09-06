package com.example.booking_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.booking_service.exception.ResourceNotFoundException;
import com.example.booking_service.service.BaseService;
import com.example.booking_service.sharedLogic.dto.ApiResponse;
import com.example.booking_service.exception.BadRequestException;
import com.example.booking_service.sharedLogic.mapper.BaseMapper;

/**
 * Shared CRUD endpoints. Subclasses declare @RestController
 * and @RequestMapping.
 */
public abstract class BaseController<E, ID, D> {

    private final BaseService<E, ID> baseService;
    private final BaseMapper<E, D> mapper;

    protected BaseController(BaseService<E, ID> baseService, BaseMapper<E, D> mapper) {
        this.baseService = baseService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody E entity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Created successfully", baseService.create(entity), true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@RequestBody D data, @PathVariable("id") ID id) {
        if (!baseService.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found");
        }
        E entity = mapper.toEntity(data);
        return ResponseEntity.ok(new ApiResponse("Updated successfully", baseService.update(entity), true));
    }

    @PutMapping("/batch")
    public ResponseEntity<ApiResponse> updateAll(@RequestBody List<D> data) {
        if (data == null || data.isEmpty() || data.stream().anyMatch(item -> item == null)) {
            throw new BadRequestException("Update list must not be empty or contain null items");
        }
        List<E> entities = data.stream().map(mapper::toEntity).toList();
        List<D> result = baseService.updateAll(entities).stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(new ApiResponse("Updated successfully", result, true));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> findAll() {
        return ResponseEntity.ok(new ApiResponse("Success", baseService.findAll(), true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> findById(@PathVariable("id") ID id) {
        return ResponseEntity.ok(new ApiResponse("Success", baseService.findById(id), true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteById(@PathVariable("id") ID id) {
        baseService.deleteById(id);
        return ResponseEntity.ok(new ApiResponse("Deleted successfully", null, true));
    }
}
