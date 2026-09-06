package com.example.user_service.controller;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.user_service.config.ApiResponseAdvice;
import com.example.user_service.exception.GlobalExceptionHandler;
import com.example.user_service.service.BaseService;
import com.example.user_service.sharedLogic.mapper.BaseMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BaseControllerTests {
    private JpaRepository<Item, Long> repository;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(JpaRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new ItemController(new BaseService<>(repository)))
                .setControllerAdvice(new GlobalExceptionHandler(), new ApiResponseAdvice()).build();
    }

    @Test
    void createAndUpdateDeserializeEntityAndCallService() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.save(any(Item.class))).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post("/items").contentType("application/json").content("{\"id\":1,\"name\":\"Test\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test"));
        mvc.perform(put("/items/1").contentType("application/json").content("{\"id\":1,\"name\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Updated"));
        verify(repository).save(new Item(1L, "Test"));
        verify(repository).save(new Item(1L, "Updated"));
    }

    @Test
    void readsListAndConvertsPathIdToLong() throws Exception {
        Item item = new Item(1L, "Test");
        when(repository.findAll()).thenReturn(List.of(item));
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        mvc.perform(get("/items")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
        mvc.perform(get("/items/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
        verify(repository).findAll();
        verify(repository).findById(1L);
    }

    @Test
    void deletesExistingEntityAndReturnsEnvelope() throws Exception {
        Item item = new Item(1L, "Test");
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        mvc.perform(delete("/items/1")).andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Deleted successfully\",\"data\":null,\"success\":true}"));
        verify(repository).delete(item);
    }

    @Test
    void missingReadAndDeleteReturn404WithoutDeleting() throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/items/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(delete("/items/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        verify(repository, never()).delete(any(Item.class));
    }

    @Test
    void batchMapsListAndSavesAll() throws Exception {
        when(repository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(put("/items/batch").contentType("application/json")
                .content("[{\"id\":1,\"name\":\"One\"},{\"id\":2,\"name\":\"Two\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("One"))
                .andExpect(jsonPath("$.data[1].id").value(2));
        verify(repository).saveAll(List.of(new Item(1L, "One"), new Item(2L, "Two")));
    }

    @Test
    void batchRejectsEmptyAndNullItemsBeforeSaving() throws Exception {
        for (String body : List.of("[]", "[null]", "null")) {
            mvc.perform(put("/items/batch").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verify(repository, never()).saveAll(anyList());
    }
    record Item(Long id, String name) { }

    @RestController
    @RequestMapping("/items")
    static class ItemController extends BaseController<Item, Long, Item> {
        ItemController(BaseService<Item, Long> service) { super(service, new BaseMapper<Item, Item>() { public Item toEntity(Item item) { return item; } public Item toDto(Item item) { return item; } }); }
    }
}
