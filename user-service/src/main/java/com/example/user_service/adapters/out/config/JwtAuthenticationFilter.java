// package com.example.user_service.adapters.out.config;


// import jakarta.annotation.Nonnull;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// import java.io.IOException;
// import java.util.Arrays;
// import java.util.Collection;
// import java.util.List;
// import java.util.stream.Collectors;

// @Component
// public class JwtAuthenticationFilter extends OncePerRequestFilter {

//     @Override
//     protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain)
//             throws ServletException, IOException {

//         final String userId = request.getHeader("X-User-ID");
//         final String userRoles = request.getHeader("X-User-Roles"); // Roles có thể là chuỗi "Admin,User"
//         if (userId != null && !userId.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
//             Collection<? extends GrantedAuthority> authorities = List.of(); // Mặc định không có quyền
//             if (userRoles != null && !userRoles.isEmpty()) {
//                 authorities = Arrays.stream(userRoles.split(","))
//                                     .map(String::trim) // Loại bỏ khoảng trắng
//                                     .filter(role -> !role.isEmpty()) // Lọc bỏ chuỗi rỗng nếu có
//                                     .map(SimpleGrantedAuthority::new)
//                                     .collect(Collectors.toList());
//             }

//             UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                     userId, 
//                     null,   
//                     authorities);

//             authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//             SecurityContextHolder.getContext().setAuthentication(authentication);
//         }

//         filterChain.doFilter(request, response);
//     }

// }