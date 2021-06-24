package com.example.salvo.services;

import com.example.salvo.models.Game;

import java.util.List;

public interface GameService {
    Game saveGame(Game game);
    List<Game> getGame();
    Game updateGame(Game game);
    Boolean deleteGame(Long id);
    Game findGameById(Long id);

}
