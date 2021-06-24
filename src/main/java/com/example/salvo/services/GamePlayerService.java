package com.example.salvo.services;

import com.example.salvo.models.GamePlayer;
import com.example.salvo.repositories.GamePlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


public interface GamePlayerService {

    GamePlayer saveGamePlayer(GamePlayer saveGamePlayer);
    List<GamePlayer> getGamePlayer();
    GamePlayer updateGamePlayer(GamePlayer gamePlayer);
    Boolean deleteGamePlayer(Long id);
    GamePlayer findGamePlayerById(Long id);
}
