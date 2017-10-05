package com.salesmanager.shop.store.services;


import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.user.PermissionService;
import com.salesmanager.core.business.services.user.UserService;
import com.salesmanager.core.model.user.Group;
import com.salesmanager.core.model.user.Permission;
import com.salesmanager.core.model.user.User;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.controller.customer.facade.CustomerFacadeImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.salesmanager.shop.store.controller.customer.facade.CustomerFacadeImpl.ROLE_PREFIX;

@Controller
@RequestMapping("/api")
public class UserApiController extends BaseApiController {


    @Inject
    private UserService userService;

    @Inject
    @Named("passwordEncoder")
    private PasswordEncoder passwordEncoder;

    @Inject
    private PermissionService permissionService;
    @Inject
    private AuthenticationManager adminAuthenticationManager;

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CustomerFacadeImpl.class);

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public HttpServletResponse login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HashMap<String, Object> map = new HashMap();


        final String userName = request.getParameter("username");
        final String password = request.getParameter("password");

        final User user = authenticate(userName, password);
        if(user != null) {
            String compactJws = Jwts.builder()
                    .setSubject(user.getAdminName())
                    .signWith(SignatureAlgorithm.HS512, "abc123")
                    .compact();
            Map entry  = new HashMap<>();
            entry.put("user_id", user.getId());
            entry.put("user_name", user.getAdminName());
            entry.put("token", compactJws);

            map.put("data", entry);
            map.put("meta", getMeta(0, 200, ""));

            setResponse(response, map);
        }

        else {
            map = getErrorResponse(getMeta(400, 400, "Invalid Credentials"));
            setResponse(response, map);
        }

        return response;
    }

    private User authenticate(final String userName, final String password){
        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        User user;
        try {
            user = userService.getByUserName(userName);
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }

        if(user == null) {
            return null;
        }

        GrantedAuthority role = new SimpleGrantedAuthority(ROLE_PREFIX + Constants.PERMISSION_AUTHENTICATED);//required to login
        authorities.add(role);
        List<Integer> groupsId = new ArrayList<Integer>();
        List<Group> groups = user.getGroups();
        if(groups!=null) {
            for(Group group : groups) {
                groupsId.add(group.getId());

            }
            if(groupsId!=null && groupsId.size()>0) {
                List<Permission> permissions = null;
                try {
                    permissions = permissionService.getPermissions(groupsId);
                } catch (ServiceException e) {
                    e.printStackTrace();
                    return null;
                }
                for(Permission permission : permissions) {
                    GrantedAuthority auth = new SimpleGrantedAuthority(permission.getPermissionName());
                    authorities.add(auth);
                }
            }
        }

        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(userName, password, authorities);

        try{
            Authentication authentication = adminAuthenticationManager.authenticate(authenticationToken);
            if(!authentication.isAuthenticated()) user = null;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return user;
    }

}
