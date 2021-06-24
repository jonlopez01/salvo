package com.example.salvo.services;


import com.example.salvo.models.Player;

import java.util.List;

public interface PlayerService {
    Player savePlayer(Player savePlayer);
    //List<Player> getPlayer();
    Player updatePlayer(Player Player);
    Boolean deleteGamePlayer(Long id);
    Player findPlayerById(Long id);
    Player findByUsername(String username);

}
