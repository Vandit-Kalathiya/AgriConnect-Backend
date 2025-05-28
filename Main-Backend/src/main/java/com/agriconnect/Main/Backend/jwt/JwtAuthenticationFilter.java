package com.agriconnect.Main.Backend.jwt;

import com.agriconnect.Main.Backend.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{
	@Autowired
	private JwtAuthenticationHelper jwtHelper;

	@Autowired
	private CustomUserDetailsService studentCustomUserDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestHeader = request.getHeader("Authorization");
		String phoneNumber = null;
		String token = null;

		Cookie cookies[] = request.getCookies();
		System.out.println("Outside...........");
		if (cookies != null) {
			System.out.println("Cookies is not nulll..........");
			for (Cookie cookie : cookies) {
//				System.out.println(cookie.getName()+" : "+cookie.getValue());
				if ("jwt_token".equals(cookie.getName())) {
					String jwtToken = cookie.getValue();
					phoneNumber = jwtHelper.getMobileNumberFromToken(jwtToken);
					if (!jwtHelper.isTokenExpired(jwtToken) && !jwtHelper.isBlacklisted(jwtToken) && phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
						UserDetails userDetails = studentCustomUserDetailsService.loadUserByUsername(phoneNumber);
						UsernamePasswordAuthenticationToken authToken =
								new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
//						System.out.println("-----------------done------------------");
					}
					break;
				}
			}
		}
		else if (requestHeader != null && requestHeader.startsWith("Bearer ")) {
			token = requestHeader.substring(7);
			phoneNumber = jwtHelper.getMobileNumberFromToken(token);

			if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null && !jwtHelper.isBlacklisted(token)) {
				UserDetails userDetails = studentCustomUserDetailsService.loadUserByUsername(phoneNumber);

				if (!jwtHelper.isTokenExpired(token)) {
					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		}

		filterChain.doFilter(request, response);
	}
}