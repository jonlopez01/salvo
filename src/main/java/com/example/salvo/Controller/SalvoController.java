package com.example.salvo.Controller;

import com.example.salvo.models.*;
import com.example.salvo.repositories.*;
import com.example.salvo.services.implementations.PlayerServiceImplement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SalvoController {
    @Autowired
    GamePlayerRepository gamePlayerRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    PlayerServiceImplement playerServiceImplement;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ShipRepository shipRepository;

    @Autowired
    SalvoRepository salvoRepository;

    @Autowired
    ScoreRepository scoreRepository;

    @GetMapping("/games")
    public Map<String, Object> getGames(Authentication authentication){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        if(isGuest(authentication)){
            dto.put("player", "Guest");
        }else {
            Player player = playerServiceImplement.findByUsername(authentication.getName());
            dto.put("player", makePlayerDto(player));
        }
        dto.put("games", gameRepository.findAll().stream().map(game -> dtoMakeGame(game)).collect(Collectors.toList()));
        return dto;
    }


    @PostMapping("/games")
    public ResponseEntity<Map<String, Object>> createGames(Authentication authentication){
        Player player = playerServiceImplement.findByUsername(authentication.getName());
        if(!isGuest(authentication)){
            Game game = new Game();
            gameRepository.save(game);
            GamePlayer gp1 = gamePlayerRepository.save(new GamePlayer(player, game));
            Map<String, Object> response = new LinkedHashMap<String, Object>();
            return new ResponseEntity<>(makeMap("gpid", gp1.getId()), HttpStatus.CREATED);
        }else{
            return new ResponseEntity<>(makeMap("error", "problemas de permisos"), HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/game_view/{id}")
    public ResponseEntity<Map<String, Object>> getGames(@PathVariable("id") Long id, Authentication authentication){
        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "No existe usuario loggeado"), HttpStatus.UNAUTHORIZED);
        }

        Player player = playerServiceImplement.findByUsername(authentication.getName());

            try{
                Optional<GamePlayer> gamePlayer = gamePlayerRepository.findById(id);
                if(gamePlayer.isPresent()) {
                    GamePlayer gp = gamePlayer.get();
                    if(player.getId() == gp.getPlayer().getId()){
                        return new ResponseEntity<>(dtoMakeGameView(gp), HttpStatus.ACCEPTED);
                    }
                }else{
                    return new ResponseEntity<>(makeMap("error", "El juego no existe"), HttpStatus.FORBIDDEN);
                }
            }catch (Exception e){
                return new ResponseEntity(makeMap("error", "error en la consulta"),HttpStatus.BAD_REQUEST);
            }

        return new ResponseEntity(makeMap("error", "error en la consulta"),HttpStatus.BAD_REQUEST);
    }



    @PostMapping("/players")
    public ResponseEntity<Object> register(@RequestParam String username, @RequestParam String password){
        if(username.isEmpty() || password.isEmpty()){
            return new ResponseEntity<>("Missin data", HttpStatus.FORBIDDEN);
        }

        if(playerServiceImplement.findByUsername(username) != null){
            return new ResponseEntity<>("Name already in use", HttpStatus.FORBIDDEN);
        }

        playerServiceImplement.savePlayer(new Player(username, passwordEncoder.encode(password)));

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    //@RequestMapping(value = "/games/players/{gamePlayerId}/ships", method = RequestMethod.POST)
    @PostMapping("/games/players/{gamePlayerId}/ships")
    public ResponseEntity<Object> addShip(@PathVariable Long gamePlayerId, @RequestBody Set<Ship> ships, Authentication authentication){

        Player player = playerServiceImplement.findByUsername(authentication.getName());

        if(isGuest(authentication)){
            System.out.println("isguest");
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Optional<GamePlayer> gamePlayer =gamePlayerRepository.findById(gamePlayerId);
        GamePlayer gp;

        if(gamePlayer.isPresent()){
            gp = gamePlayer.get();
        }else {
            return new ResponseEntity<>(makeMap("error", "gameplayer no existe"),HttpStatus.FORBIDDEN);
        }

        if(player.getId() != gp.getPlayer().getId()){
            return new ResponseEntity<>(makeMap("error", "el id del player no corresponde"),HttpStatus.FORBIDDEN);
        }


        if(gp.getShips().size() > 0){
            return new ResponseEntity<>(makeMap("error","Ya se establecieron las naves"),HttpStatus.FORBIDDEN);
        }else {
            for(Ship ship : ships){
                gp.addShip(ship);
                shipRepository.save(ship);

            }

            return new ResponseEntity<>(makeMap("OK", "naves agregadas"),HttpStatus.CREATED);
        }

    }

    @PostMapping("/game/{idGame}/players")
    public ResponseEntity<Object> joinGame(@PathVariable ("idGame")Long idGame, Authentication authentication){
        if(isGuest(authentication)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Player player = playerServiceImplement.findByUsername(authentication.getName());
        Optional<Game> game = gameRepository.findById(idGame);

        //Validar si el juego existe
        if(game.isPresent() && player != null) {
            Game game1 = game.get();
            Set<GamePlayer> listGp =game1.getGamePlayers();

            // Validar si el player ya esta unido al juego
            boolean existGp= false;
            for (GamePlayer gp:listGp) {
                if(gp.getPlayer().getId() == player.getId()){
                    existGp = true;
                }
            }
            //Validar que el juego tiene 1 jugador

            if(game1.getGamePlayers().size() == 1 && existGp == false){
                GamePlayer gp1 = new GamePlayer(player,game1);
                gamePlayerRepository.save(gp1);
                return new ResponseEntity<>(makeMap("gpid", gp1.getId()), HttpStatus.CREATED);
            }

        }
        return new ResponseEntity<>(makeMap("error", "El juago no admite mas jugadores"),HttpStatus.FORBIDDEN);

    }

    @PostMapping("/games/players/{gamePlayerId}/salvoes")
    public ResponseEntity<Object> addSalvo(@PathVariable Long gamePlayerId, @RequestBody Salvo salvo, Authentication authentication){
        System.out.println("ingreso a metodo addSalvo: salvo-shot posicion 1:" + salvo.getSalvoLocations().get(0));
        //Validar si un usuario esta loggeado
        if(isGuest(authentication)){
            System.out.println("Sin session iniciada");
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        //valudar que el salvo tiene entre 1 y 5 cells
        if(salvo.getSalvoLocations().size() == 0 || salvo.getSalvoLocations().size() > 5){
            System.out.println("validacion salvo 1-5 tiros");
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }


        //Validar que el gameplayer enviado por parametro
        Optional<GamePlayer> gamePlayer =gamePlayerRepository.findById(gamePlayerId);
        GamePlayer gp;
        System.out.println("validar gameplayer enviado por parametro");
        if(gamePlayer.isPresent()){
            gp = gamePlayer.get();
        }else {
            return new ResponseEntity<>(makeMap("error", "gameplayer no existe"),HttpStatus.FORBIDDEN);
        }

        //Validar que el player coincide con el gameplayer
        Player player = playerServiceImplement.findByUsername(authentication.getName());
        System.out.println("validar player coincide con gameplayer");
        if(player.getId() != gp.getPlayer().getId()){
            return new ResponseEntity<>(makeMap("error", "el id del player no corresponde"),HttpStatus.FORBIDDEN);
        }

        //Establecer los turnos
        int turn = 0;
        if(gp.getSalvos().size() == 0){
            turn += 1;
        }else{
           turn = gp.getSalvos().size() +1;
        }

        //recorrer los gameplayer del Game, para agregar el gameplayer que corresponde al salvo y solo si la cantidad de salvo
        //es menor o igual

        Set<GamePlayer> gamePlayers = gp.getGame().getGamePlayers();
        GamePlayer gamePlayerOponente = null;
        for (GamePlayer gamePlayer1: gamePlayers) {
            if (player.getId() != gamePlayer1.getPlayer().getId()) {
                gamePlayerOponente = gamePlayer1;
            }
        }

        if(gp.getSalvos().size() > gamePlayerOponente.getSalvos().size()){
            return new ResponseEntity<>(makeMap("error", "ya se ha enviado salvo para este turno"),HttpStatus.FORBIDDEN);
        }

            System.out.println("recorrer GAME");
            salvo.setGamePlayer(gp);
            salvo.setTurn(turn);
            salvoRepository.save(salvo);
            System.out.println("persistir salvo");
            return new ResponseEntity<>(makeMap("OK", "Se agrego el salvo"),HttpStatus.CREATED);




        }






    private boolean isGuest(Authentication authentication){
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    /*private Map<String, Object> dtoMakeGameView(GamePlayer gamePlayer){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        dto.put("gamePlayers", gamePlayer.getGame().getGamePlayers().stream().map(gP -> dtoMakeGamePlayer(gP)).collect(Collectors.toList()));
        dto.put("ships", gamePlayer.getShips().stream().map(ship -> dtoMakeShip(ship)).collect(Collectors.toList()));
        dto.put("salvoes",  gamePlayer.getGame().getGamePlayers().stream().flatMap(gamePlayer1 -> gamePlayer1.getSalvos().stream()
                .map(salvo -> dtoMakeSalvos(salvo)))
                .collect(Collectors.toList()));
        return dto;
    }*/

    private Map<String, Object> dtoMakeGameView(GamePlayer gamePlayer){
        Game game = gamePlayer.getGame();
        GamePlayer opponent = new GamePlayer();
        Map<String, Object> dto = new LinkedHashMap<>();
        Map<String, Object> hits = new LinkedHashMap<>();

        String state = this.stateGame(gamePlayer);

        if(gamePlayer.getOpponent() != null){
            hits.put("self", this.getHits(gamePlayer, gamePlayer.getOpponent()));
            hits.put("opponent", this.getHits(gamePlayer.getOpponent(),gamePlayer));
        }else{
            hits.put("self", new ArrayList<>());
            hits.put("opponent", new ArrayList<>());
        }
        dto.put("id", gamePlayer.getGame().getId());
        dto.put("created", gamePlayer.getGame().getCreationDate());
        dto.put("gameState", state);
        dto.put("gamePlayers", gamePlayer.getGame().getGamePlayers().stream().map(gP -> dtoMakeGamePlayer(gP)).collect(Collectors.toList()));
        dto.put("ships", gamePlayer.getShips().stream().map(ship -> dtoMakeShip(ship)).collect(Collectors.toList()));
        dto.put("salvoes",  gamePlayer.getGame().getGamePlayers().stream().flatMap(gamePlayer1 -> gamePlayer1.getSalvos().stream()
                .map(salvo -> dtoMakeSalvos(salvo)))
                .collect(Collectors.toList()));
        dto.put("hits", hits);
        return dto;
    }

    private Map<String, Object> dtoMakeSalvos(Salvo salvo){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("turn", salvo.getTurn());
        dto.put("locations", salvo.getSalvoLocations());
        dto.put("player", salvo.getGamePlayer().getPlayer().getId());

        return dto;
    }



    private Map<String, Object> dtoMakeSalvos(GamePlayer gamePlayer){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("turn", gamePlayer.getSalvos().stream().map(salvo -> salvo.getTurn()).collect(Collectors.toList()));
        dto.put("locations", gamePlayer.getSalvos().stream().map(salvo -> salvo.getSalvoLocations()).collect(Collectors.toList()));
        dto.put("player", gamePlayer.getPlayer().getId());

        return dto;
    }



    private Map<String, Object> dtoMakeShip(Ship ship){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("type", ship.getType());
        dto.put("locations", ship.getShipLocations());
        return dto;
    }

    private Map<String, Object> dtoMakeGame(Game game){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId());
        dto.put("created", game.getCreationDate());
        dto.put("gamePlayers", game.getGamePlayers().stream().map(gamePlayer -> dtoMakeGamePlayer(gamePlayer)).collect(Collectors.toList()));
        dto.put("scores", game.getScores().stream().map(score -> makeScoreDTO(score)).collect(Collectors.toList()));
        return dto;
    }

    public Map<String,  Object> makeScoreDTO(Score  score){
        Map<String, Object> dto = new HashMap<>();
        dto.put("score", score.getScore());
        dto.put("player", score.getPlayerID().getId());
        return  dto;
    }

    private Map<String, Object> dtoMakeGamePlayer(GamePlayer gamePlayer){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getId());
        dto.put("player", makePlayerDto(gamePlayer.getPlayer()));
        return dto;
    }

    private Map<String, Object> makePlayerDto(Player player){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        return dto;
    }

    private Map<String, Object> makeMap(String key, Object value){
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private List<String> getLocationsByType(String type, GamePlayer self){
        return self.getShips().size() == 0 ? new ArrayList<>() : self.getShips().stream()
                .filter(ship -> ship.getType().equals(type)).findFirst().get().getShipLocations();
    }

    private List<Map> getHits(GamePlayer self, GamePlayer opponent){
        List<Map> hits = new ArrayList<>();

        Integer carrierDamage = 0;
        Integer battleshipDamage = 0;
        Integer submarineDamage = 0;
        Integer destroyerDamage = 0;
        Integer patrolboatDamage = 0;

        List<String> carrierLocations = getLocationsByType("carrier", self);
        List<String> battleshipLocations = getLocationsByType("battleship", self);
        List<String> submarineLocations = getLocationsByType("submarine", self);
        List<String> destroyerLocations = getLocationsByType("destroyer", self);
        List<String> patrolboatLocations = getLocationsByType("patrolboat", self);

        //Recorrer cada Salvo
        for (Salvo salvo: opponent.getSalvos()) {
        //for (Salvo salvo: self.getSalvos()) {

            int carrierHitsInTurn= 0;
            int battleshipHitsInTurn= 0;
            int submarineHitsInTurn= 0;
            int destroyerHitsInTurn= 0;
            int patrolboatHitsInTurn= 0;

            int missedShots = salvo.getSalvoLocations().size();



            Map<String, Object> hitsMapPerTurn = new LinkedHashMap<>();
            Map<String, Object> damagesMapPerTurn = new LinkedHashMap<>();

            List<String> salvoLocationsLit = new ArrayList<>();
            List<String> hitCellsList = new ArrayList<>(); // List Locations



            //Recorrer cada location de salvoLocations dentro de Salvo
            for(String salvoShot : salvo.getSalvoLocations()){

                if(carrierLocations.contains(salvoShot)){
                    carrierDamage++;
                    carrierHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }

                if(battleshipLocations.contains(salvoShot)){
                    battleshipDamage++;
                    battleshipHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }

                if(submarineLocations.contains(salvoShot)){
                    submarineHitsInTurn++;
                    submarineDamage++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if(destroyerLocations.contains(salvoShot)){
                    destroyerHitsInTurn++;
                    destroyerDamage++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if(patrolboatLocations.contains(salvoShot)){
                    patrolboatHitsInTurn++;
                    patrolboatDamage++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }


            } //Fin de for(String location : salvo.getSalvoLocations()){


            damagesMapPerTurn.put("carrierHits", carrierHitsInTurn);
            damagesMapPerTurn.put("battleshipHits", battleshipHitsInTurn);
            damagesMapPerTurn.put("submarineHits", submarineHitsInTurn);
            damagesMapPerTurn.put("destroyerHits", destroyerHitsInTurn);
            damagesMapPerTurn.put("patrolboatHits", patrolboatHitsInTurn);

            damagesMapPerTurn.put("carrier", carrierDamage);
            damagesMapPerTurn.put("battleship", battleshipDamage);
            damagesMapPerTurn.put("submarine", submarineDamage);
            damagesMapPerTurn.put("destroyer", destroyerDamage);
            damagesMapPerTurn.put("patrolboat", patrolboatDamage);


            hitsMapPerTurn.put("turn", salvo.getTurn());
            hitsMapPerTurn.put("hitLocations", hitCellsList);
            hitsMapPerTurn.put("damages", damagesMapPerTurn);
            hitsMapPerTurn.put("missed", missedShots);

            hits.add(hitsMapPerTurn);
        }

        return hits;
    }

    private String stateGame(GamePlayer gamePlayer){

        GamePlayer opponent = gamePlayer.getOpponent();



        //Validar si se agregaron los barcos
        if(gamePlayer.getShips().isEmpty()){
            return "PLACESHIPS";
        }

        if(opponent == null || opponent.getShips().isEmpty()){
            return "WAITINGFOROPP";
        }

        if(gamePlayer.getSalvos().size() > opponent.getSalvos().size()){
            return "WAIT";
        }

        boolean sunkenShipsSelft = this.sunkenShips(gamePlayer);
        boolean sunkenShipsOpponent = this.sunkenShips(opponent);

        if(sunkenShipsSelft == false && sunkenShipsOpponent == false){
            return "PLAY";
        }


        if(sunkenShipsSelft == true && sunkenShipsOpponent == true){
            return "TIE";
        }

        if(sunkenShipsSelft == true){
            Score score = new Score(0.0, new Date(), gamePlayer.getPlayer(), gamePlayer.getGame());
            scoreRepository.save(score);
            return "LOST";
        }else{
            Score score = new Score(1.0, new Date(), gamePlayer.getPlayer(), gamePlayer.getGame());
            scoreRepository.save(score);
            return "WON";
        }




    }

    private boolean sunkenShips(GamePlayer gamePlayer){

        GamePlayer opponent = gamePlayer.getOpponent();

        if(!opponent.getShips().isEmpty() && !gamePlayer.getSalvos().isEmpty()){
            return opponent.getSalvos().stream().flatMap(salvo -> salvo.getSalvoLocations().stream()).collect(Collectors.toList()).containsAll(gamePlayer.getShips().stream()
                    .flatMap(ship -> ship.getShipLocations().stream()).collect(Collectors.toList()));
        }
        return false;
    }
}
