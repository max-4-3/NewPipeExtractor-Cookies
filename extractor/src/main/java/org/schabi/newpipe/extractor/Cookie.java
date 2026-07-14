package org.schabi.newpipe.extractor;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Cookie {
    private String name;
    private String value;

    private String domain;
    private String path = "/";

    private long expiresAt = -1L;      // epoch millis, -1L = session cookie
    private boolean secure;
    private boolean httpOnly;
    private boolean hostOnly;


    @Nullable
    public static Cookie parseSetCookie(String header, URL url) {
        if (header == null) {
            return null;
        }

        String[] parts = header.split(";");
        if (parts.length < 1) {
            return null;
        }

        Cookie cookie = new Cookie();
        for (int i = 0; i < parts.length; ++i) {
            int idx = parts[i].indexOf('=');
            if (idx == -1) {
                // invalid cookie
                return null;
            }

            String key = parts[i].substring(0, idx).trim();
            String value = parts[i].length() - 1 <= idx ? "TRUE" : parts[i].substring(idx + 1).trim();

            if (i == 0) {
                cookie.name = key;
                cookie.value = value;
            } else {
                switch (key.toLowerCase()) {
                    case "domain":
                        cookie.domain = value;
                        break;
                    case "path":
                        cookie.path = value;
                        break;
                    case "secure":
                        cookie.secure = fromBool(value);
                        break;
                    case "httponly":
                        cookie.httpOnly = fromBool(value);
                        break;
                    case "max-age":
                        cookie.expiresAt = System.currentTimeMillis() + Long.parseLong(value) * 1000L;
                        break;
                    case "expires":
                        cookie.expiresAt = parseExpires(value);
                        break;
                    default:
                        break;
                }
            }
        }

        if (cookie.domain == null || cookie.domain.isEmpty()) {
            cookie.domain = url.getHost();
            cookie.hostOnly = true;
        } else {
            cookie.hostOnly = false;
        }

        return cookie;
    }

    private static final SimpleDateFormat[] DATE_FORMATS = {
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
        new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US),
        new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss z", Locale.US)
    };

    static {
        for (SimpleDateFormat sdf: DATE_FORMATS) {
            sdf.setLenient(true);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    private static long parseExpires(String value) {
        for (SimpleDateFormat sdf : DATE_FORMATS) {
            try {
                Date date = sdf.parse(value);
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        if (true) {
            return -1L;
        } else {
            throw new IllegalArgumentException("Invalid cookie expiry: " + value);
        }
    }

    @Nonnull
    public String toCookieHeader() {
        return name + "=" + value;
    }

    @Nullable
    public static Cookie parseNetscape(String line) {
        if (line == null || line.startsWith("#") || line.trim().isEmpty()) {
            return null;
        }

        String[] p = line.split("\t", -1);
        if (p.length != 6 && p.length != 7) {
            return null;
        }

        Cookie c = new Cookie();

        c.domain = p[0];
        c.hostOnly = !"TRUE".equalsIgnoreCase(p[1]);
        c.path = p[2];
        c.secure = "TRUE".equalsIgnoreCase(p[3]);

        long expires = Long.parseLong(p[4]);
        c.expiresAt = expires == 0 ? -1 : expires * 1000L;

        c.name = p[5];
        c.value = p.length == 7 ? p[6] : "";

        return c;
    }

    @Nonnull
    public String toNetscape() {
        long exp = expiresAt == -1L ? 0 : expiresAt / 1000L;
        String[] parts = new String[] {
            domain,
            toBool(!hostOnly),
            path,
            toBool(secure),
            String.valueOf(exp),
            name,
            value
        };

        return String.join("\t", parts);
    }

    private static String toBool(boolean value) {
        return value ? "TRUE" : "FALSE";
    }

    private static boolean fromBool(String value) {
        return value.equalsIgnoreCase("true");
    }

    public boolean isExpired() {
        return expiresAt != -1L && System.currentTimeMillis() >= expiresAt;
    }

    public boolean matches(URL url) {
        if (isExpired()) {
            return false;
        }

        if (secure && !"https".equalsIgnoreCase(url.getProtocol())) {
            return false;
        }

        String host = url.getHost();
        if (hostOnly) {
            if (!host.equalsIgnoreCase(domain)) {
                return false;
            }
        } else {
            if (!host.equalsIgnoreCase(domain) && !host.endsWith("." + domain.replaceFirst("^\\.", ""))) {
                return false;
            }
        }

        return url.getPath().startsWith(path);
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Cookie (" + "name=" + getName() + ", value=" + getValue() + ", domain=" + getDomain() + ", path=" + path + ", expiresAt=" + expiresAt + ", secure=" + secure + ", httpOnly=" + httpOnly + ", hostOnly=" + hostOnly + ')';
    }
}