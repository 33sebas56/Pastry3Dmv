package com.backend.pastry3d.history.service;

import com.backend.pastry3d.history.entity.History;
import com.backend.pastry3d.history.repository.HistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoryService {
    private final HistoryRepository historyRepository;

    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public History log(Long userId, Long recipeId, String action) {
        History history = new History();
        history.setUserId(userId);
        history.setRecipeId(recipeId);
        history.setAction(action);
        return historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<History> findMine(Long userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
