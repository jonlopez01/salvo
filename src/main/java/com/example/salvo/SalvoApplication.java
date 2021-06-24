package com.example.salvo;

import com.example.salvo.models.*;
import com.example.salvo.repositories.*;
import com.example.salvo.services.implementations.PlayerServiceImplement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Date;

@SpringBootApplication
public class SalvoApplication {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Autowired
	PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(SalvoApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(PlayerRepository playerRepository,
									  GameRepository gameRepository, GamePlayerRepository gamePlayerRepository,
									  ShipRepository shipRepository, SalvoRepository salvoRepository,
									  ScoreRepository scoreRepository) {
		return (args) -> {

			Player player1 = playerRepository.save(new Player("jack@gmail.com", passwordEncoder.encode("123")));
			Player player2 = playerRepository.save(new Player("frank@gmail.com", passwordEncoder.encode("123")));



			Date date = new Date();

			Game game1 = gameRepository.save(new Game());
			Game game2 = gameRepository.save(new Game(Date.from(date.toInstant().plusSeconds(3600))));
			Game game3 = gameRepository.save(new Game(Date.from(date.toInstant().plusSeconds(7200))));

			GamePlayer gamePlayer1 = gamePlayerRepository.save(new GamePlayer(player1, game1));
			GamePlayer gamePlayer2 = gamePlayerRepository.save(new GamePlayer(player2, game1));

			Ship ship1 = new Ship(gamePlayer1, Arrays.asList("H1","H2","H3"), "Crucero" );
			Ship ship2 = new Ship(gamePlayer2, Arrays.asList("C1","C2","C3"), "Crucero" );

			shipRepository.save(ship1);
			shipRepository.save(ship2);

			Salvo salvo1 = salvoRepository.save(new Salvo(1, gamePlayer1, Arrays.asList("A1","B2","C3")));
			Salvo salvo2 = salvoRepository.save(new Salvo(2, gamePlayer2, Arrays.asList("A2","D2","D3")));

			Score score1 = scoreRepository.save(new Score(0.0, date, player1,game1));
			Score score2 = scoreRepository.save(new Score(1.0, date, player2,game1));


		};
	}
}

@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	PlayerServiceImplement playerServiceImplement;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputName-> {
			Player player = playerServiceImplement.findByUsername(inputName);
			if (player != null) {
				return new User(player.getUserName(), player.getPassword(),
						AuthorityUtils.createAuthorityList("USER"));
			}else if(player.getId() == 2){
				return new User(player.getUserName(), player.getPassword(),
						AuthorityUtils.createAuthorityList("ADMIN"));
			}
			else {
				throw new UsernameNotFoundException("Unknown user: " + inputName);
			}
		});
	}
}

@EnableWebSecurity
@Configuration
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		/*http.authorizeRequests().antMatchers("/" , "/web/css/**", "/web/js/**", "/web/images/**","/api/games", "/web/games.html", "/web/login.html", "/api/login").permitAll().
		antMatchers("/h2-console/**").permitAll().anyRequest().authenticated().
		antMatchers("/api/game_view/**","/api/game/**").hasAuthority("USER");*/

		http.authorizeRequests()
				.antMatchers("/api/games", "/api/players", "/api/login").permitAll()
				.antMatchers("/web/game.html").hasAuthority("USER")
				.antMatchers("/api/game_view/**").hasAuthority("USER")
				.antMatchers("/api/games/players/**").hasAuthority("USER")
				.antMatchers("/web/**").permitAll()
				.antMatchers("/h2-console/**").permitAll().anyRequest().authenticated()
				.and().csrf().ignoringAntMatchers("/h2-console/**")
				.and().headers().frameOptions().sameOrigin();


		http.formLogin()
			.usernameParameter("name")
			.passwordParameter("pwd")
			.loginPage("/api/login");

		http.logout().logoutUrl("/api/logout");

		// turn off checking for CSRF tokens
		http.csrf().disable();

		// if user is not authenticated, just send an authentication failure response
		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		// if login is successful, just clear the flags asking for authentication
		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));

		// if login fails, just send an authentication failure response
		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

		// if logout is successful, just send a success response
		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
	}

	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}


}
