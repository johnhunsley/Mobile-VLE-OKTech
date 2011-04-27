package com.mobilevle.oktech.session;

import com.mobilevle.core.moodle.User;


/**
 * <p></p>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * </p>
 * @author johnhunsley
 *         Date: 10-Nov-2010
 *         Time: 11:50:20
 */
public class Session {
    public static final String USER_ID = "USER_ID";
    public static final String TOKEN = "TOKEN";
    private String username;
    private String password;
    private String userId;
    private int clientId;
    private String token;

    /**
     *
     */
    public Session() { }

    /**
     *
     * @param username
     * @param password
     * @param userId
     * @param token
     */
    public Session(String username, String password, final String userId, String token) {
        this.username = username;
        this.password = password;
        this.userId = userId;
        this.token = token;
    }

    /**
     *
     * @param username
     * @param password
     */
    public Session(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    /**
     *
     * @return true if this session has a valid token
     */
    public boolean isValid() {
        return token != null;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    /**
     * <p>Get the username and userId wrapped as a {@link User}</p>
     * @return  {@link User}
     */
    public User asAuthenticatedUser() {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setFirstName("Me");
        return user;
    }
}
