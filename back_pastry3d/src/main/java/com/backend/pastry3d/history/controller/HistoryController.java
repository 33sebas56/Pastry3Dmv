package com.backend.pastry3d.history.controller;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.history.entity.History;
import com.backend.pastry3d.history.service.HistoryService;
import com.backend.pastry3d.shared.security.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService historyService;
    private final CurrentUserService currentUserService;

    public HistoryController(HistoryService historyService, CurrentUserService currentUserService) {
        this.historyService = historyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public List<History> findMine(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return historyService.findMine(currentUser.getId());
    }
}
