package com.optiim;

import com.sun.javafx.PlatformUtil;
import oracle.jdbc.pool.OracleDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Main {

    private static String path = System.getProperty("user.dir") + File.separator;
    private static String downloadPath = path + "downloads" + File.separator;
    private static final Logger LOGGER = LogManager.getLogger(Main.class.getName());

    private static ChromeDriver driver;

    private static Properties prop = new Properties();

    public static void main(String args[]) throws SQLException, IOException {
        LOGGER.info("Path: " + path);

        prop.load(new FileInputStream("config.properties"));

        Connection conn = getConnection(
                prop.getProperty("db.serverName"),
                Integer.valueOf(prop.getProperty("db.port")),
                prop.getProperty("db.name"),
                prop.getProperty("db.user"),
                prop.getProperty("db.password"),
                prop.getProperty("db.driverType"));

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(prop.getProperty("db.query"));

        driverConfig();
        try {
            driver.get(prop.getProperty("ppm.url"));
            driver.findElement(By.name("USERNAME")).sendKeys(prop.getProperty("ppm.user"));
            driver.findElement(By.name("PASSWORD")).sendKeys(prop.getProperty("ppm.password"), Keys.ENTER);
            driver.findElement(By.linkText("Sign Out"));
        } catch (Exception e) {
            LOGGER.error(String.format("Login to PPM error: %s", e.getCause()));
            return;
        }
        String cookies = getCookies();
        driver.quit();

        int i = 0;
        while (rs.next()) {
            String reqId = rs.getString("request_id");
            String url = rs.getString("url");
            String docId = rs.getString("document_id");
            String name = rs.getString("name");
            String extension = rs.getString("extension");
            String file = String.format("%s_%s_%s.%s", reqId, docId, name, extension);
            try {
                URLConnection con = new URL(url).openConnection();
                con.setRequestProperty("Cookie", cookies);

                FileUtils.copyInputStreamToFile(con.getInputStream(), new File(downloadPath + file));

                LOGGER.info(String.format("%s, %s", file, url));
                i++;
            } catch (IOException e) {
                LOGGER.error(String.format("%s, %s    :error: %s", file, url, e.getCause()));
            }
        }
        System.out.println("Downloaded files count: " + i);
        rs.close();
        stmt.close();
        conn.close();
    }

    private static void driverConfig() {
        String d = PlatformUtil.isWindows() ? "chromedriver.exe" : "chromedriver";
        System.setProperty("webdriver.chrome.driver", path + d);

        ChromeOptions options = new ChromeOptions();
        options.setHeadless(true);
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    private static Connection getConnection(String serverName, int portNumber, String databaseName, String user, String password, String driverType) throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setServerName(serverName);
        dataSource.setPortNumber(portNumber);
        dataSource.setDriverType(driverType);
        dataSource.setDatabaseName(databaseName);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource.getConnection();
    }

    private static String getCookies() {
        StringBuilder cookieString = new StringBuilder();
        driver.manage().getCookies().forEach(cookie ->
                cookieString
                        .append(cookie.getName()).append("=")
                        .append(cookie.getValue()).append("; ")
        );
        return cookieString.toString();
    }

}

