package com.example.salvo.services.implementations;

import com.example.salvo.models.GamePlayer;
import com.example.salvo.repositories.GamePlayerRepository;
import com.example.salvo.services.GamePlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
public class GamePlayerServiceImplement implements GamePlayerService {

    @Autowired
    GamePlayerRepository gamePlayerRepository;

    @Override
    public GamePlayer saveGamePlayer(GamePlayer saveGamePlayer) {
        return gamePlayerRepository.save(saveGamePlayer);
    }

    @Override
    public List<GamePlayer> getGamePlayer() {
        return gamePlayerRepository.findAll();
    }

    @Override
    public GamePlayer updateGamePlayer(GamePlayer gamePlayer) {
        return gamePlayerRepository.save(gamePlayer);
    }

    @Override
    public Boolean deleteGamePlayer(Long id) {
        if(findGamePlayerById(id).getId() > 0){
            gamePlayerRepository.deleteById(id);
            return true;
        }

        return null;
    }

    @Override
    public GamePlayer findGamePlayerById(Long id) {
        Optional<GamePlayer> gamePlayer = gamePlayerRepository.findById(id);
        if(gamePlayer.isPresent()){
            return gamePlayer.get();
        }
        return null;
    }
}



