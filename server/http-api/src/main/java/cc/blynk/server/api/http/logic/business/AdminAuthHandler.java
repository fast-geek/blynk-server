package cc.blynk.server.api.http.logic.business;

import cc.blynk.core.http.BaseHttpHandler;
import cc.blynk.core.http.MediaType;
import cc.blynk.core.http.Response;
import cc.blynk.core.http.annotation.Consumes;
import cc.blynk.core.http.annotation.FormParam;
import cc.blynk.core.http.annotation.POST;
import cc.blynk.core.http.annotation.Path;
import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.auth.User;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import static cc.blynk.core.http.Response.redirect;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
@Path("")
@ChannelHandler.Sharable
public class AdminAuthHandler extends BaseHttpHandler {

    //1 month
    private static final int COOKIE_EXPIRE_TIME = 30 * 60 * 60 * 24;

    private final UserDao userDao;
    private final SessionHolder sessionHolder;

    public AdminAuthHandler(Holder holder, SessionHolder sessionHolder, String adminRootPath) {
        super(holder, adminRootPath);
        this.userDao = holder.userDao;
        this.sessionHolder = sessionHolder;
    }

    @POST
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/login")
    public Response login(@FormParam("email") String email,
                          @FormParam("password") String password) {

        if (email == null || password == null) {
            return redirect(rootPath);
        }

        User user = userDao.getByName(email, AppName.BLYNK);

        if (user == null || !user.isSuperAdmin) {
            return redirect(rootPath);
        }

        if (!password.equals(user.pass)) {
            return redirect(rootPath);
        }

        Response response = redirect(rootPath);

        Cookie cookie = makeDefaultSessionCookie(sessionHolder.generateNewSession(user), COOKIE_EXPIRE_TIME);
        response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));

        return response;
    }

    @POST
    @Path("/logout")
    public Response logout() {
        Response response = redirect(rootPath);
        Cookie cookie = makeDefaultSessionCookie("", 0);
        response.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        return response;
    }

    private static Cookie makeDefaultSessionCookie(String sessionId, int maxAge) {
        DefaultCookie cookie = new DefaultCookie(Cookies.SESSION_COOKIE, sessionId);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

}