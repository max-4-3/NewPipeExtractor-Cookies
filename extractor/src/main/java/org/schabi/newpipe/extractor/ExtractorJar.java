package org.schabi.newpipe.extractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ExtractorJar {
    private final File cookieFile;
    private final List<Cookie> cookies = new ArrayList<>();

    public ExtractorJar(final File cookieFile) {
        this.cookieFile = cookieFile;
        this.cookies.addAll(this.load());
    }

    /**
     * Dumps the Cookies into the sb (separate lines) into Netscape format
     * @param sb StringBuilder to dump into
     */
    public void dump(final StringBuilder sb) {
        for (Cookie c : cookies) {
            sb.append(c.toNetscape());
            sb.append('\n');
        }
    }

    /**
     * Save cookies from a response and updates the `cookieFile` provided in constructor
     * @param url the url of the resource fetched (full url)
     * @param headers the `Set-Cookie` headers
     */
    public void saveFromResponse(@Nonnull String url, @Nonnull List<String> headers) {
        try {
            URL httpUrl = new URL(url);
            List<Cookie> cookies = new ArrayList<>();

            for (String cookie: headers) {
                Cookie cook = Cookie.parseSetCookie(cookie, httpUrl);
                if (cook != null) {
                    cookies.add(cook);
                }
            }

            this.cookies.removeIf(c -> cookies.stream().anyMatch(
                n -> n.getName().equals(c.getName()) && n.getDomain().equals(c.getDomain()))
            );
            this.cookies.addAll(cookies);
            save();
        } catch (MalformedURLException e) {
            log("ExtractorJar", "Invalid `url` passed");
        }
    }

    /**
     * Get cookies for a specific url (correctly matched)
     * @param url the url for which cookies to get
     * @return Cookies
     */
    @Nullable
    public List<Cookie> loadForRequest(@Nonnull String url) {
        final List<Cookie> cookies = new ArrayList<>();

        try {
            final URL httpUrl = new URL(url);
            cookies.addAll(this.cookies.stream().filter(c -> c.matches(httpUrl)).toList());
        } catch (MalformedURLException e) {
            log("ExtractorJar", String.format("Unable to find cookies for '%s': %s", url, e));
        }

        return cookies;
    }

    private void log(String tag, String msg) {
        System.out.printf("%s: %s%n", tag, msg);
    }

    private void save() {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(cookieFile)))) {
            final StringBuilder sb = new StringBuilder();
            sb.append("# Netscape HTTP Cookie File\n");
            sb.append("# This file was generated automatically.\n");
            sb.append('\n');
            dump(sb);

            out.println(sb);
        } catch (final IOException err) {
            log("ExtractorJar", String.format("Failed to write: %s", err));
        }
    }

    @Nonnull
    private List<Cookie> load() {
        List<Cookie> cookies = new ArrayList<>();
        if (cookieFile == null || !(cookieFile.canRead() && cookieFile.isFile())) {
            return cookies;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(cookieFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                Cookie cookie = Cookie.parseNetscape(line);

                if (cookie != null) {
                    cookies.add(cookie);
                } else {
                    log("CookieParserNetScape", String.format("Failed to parse: %s", line));
                }
            }

        } catch (final IOException err) {
            log("ExtractorJar", String.format("Failed to load: %s", err));
        }

        return cookies;
    }
}
