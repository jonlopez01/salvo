package com.example.salvo.services.implementations;

import com.example.salvo.models.Player;
import com.example.salvo.repositories.PlayerRepository;
import com.example.salvo.services.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlayerServiceImplement implements PlayerService {
    @Autowired
    PlayerRepository playerRepository;

    @Override
    public Player savePlayer(Player savePlayer) {
        return playerRepository.save(savePlayer);
    }

    @Override
    public Player updatePlayer(Player Player) {
        return playerRepository.save(Player);
    }

    @Override
    public Boolean deleteGamePlayer(Long id) {

        return null;//playerRepository.deleteById(id);
    }

    @Override
    public Player findPlayerById(Long id) {
        Optional<Player> player = playerRepository.findById(id);
        if (player.isPresent()){
            return player.get();
        }else{
            return null;
        }
    }

    @Override
    public Player findByUsername(String username) {
        Optional<Player> player = playerRepository.findByUserName(username);
        if (player.isPresent()){
            return player.get();
        }else{
            return null;
        }
    }
}
