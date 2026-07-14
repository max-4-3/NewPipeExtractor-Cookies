package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.utils.ExtractorLogger;

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

    private void saveFromResponse(@Nonnull URL ignored, @Nonnull List<Cookie> cookies) {
        this.cookies.removeIf(c -> cookies.stream().anyMatch(
            n -> n.getName().equals(c.getName()) && n.getDomain().equals(c.getDomain()))
        );
        this.cookies.addAll(cookies);
        save();
    }

    public void saveFromResponse(@Nonnull String url, @Nonnull List<String> cookies) {
        try {
            URL httpUrl = new URL(url);
            List<Cookie> cookieList = new ArrayList<>();

            for (String cookie: cookies) {
                Cookie cook = Cookie.parseSetCookie(cookie, httpUrl);
                if (cook != null) {
                    cookieList.add(cook);
                }
            }

            saveFromResponse(httpUrl, cookieList);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public List<Cookie> loadForRequest(@Nonnull String url) {
        try {
            URL httpUrl = new URL(url);
            ExtractorLogger.d("ExtractorJar", httpUrl.toString());

            return cookies.stream().filter(c -> c.matches(httpUrl)).toList();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void save() {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(cookieFile)))) {
            out.println("# Netscape HTTP Cookie File");
            out.println("# This file was generated automatically.");
            out.println();

            for (Cookie c : cookies) {
                out.print(c.toNetscape());
                out.println();
            }
        } catch (IOException ignored) {
            log("ExtractorJar", String.format("Failed to write: %s", ignored));
        }
    }

    private void log(String tag, String msg) {
        System.out.println(String.format("%s: %s", tag, msg));
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

        } catch (IOException ignored) {
            log("ExtractorJar", String.format("Failed to load: %s", ignored));
        }

        return cookies;
    }
}
