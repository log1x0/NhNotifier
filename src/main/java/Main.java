import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class Main {
    public static void main(String[] args) throws AWTException {
        if (args.length != 2 || args[0].isBlank() || args[1].isBlank()) {
            System.out.println("Please enter (the cookie) uid and pass next time.");
            return;
        }

        final SystemTray tray = SystemTray.getSystemTray();
        final Image image = Toolkit.getDefaultToolkit().createImage(Main.class.getResource("favicon.jpg"));
        final TrayIcon trayIcon = new TrayIcon(image, "Tray NH");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("System tray NH");
        tray.add(trayIcon);

        try (WebClient wc = new WebClient(BrowserVersion.FIREFOX)) {
            wc.getOptions().setThrowExceptionOnScriptError(false);
            wc.getOptions().setCssEnabled(false);
            wc.getCookieManager().addCookie(new Cookie("newheaven.nl", "uid", args[0]));
            wc.getCookieManager().addCookie(new Cookie("newheaven.nl", "pass", args[1]));
            new Timer().schedule(new TimerTask() {
                String[][] lastTable = new String[0][0];

                @Override
                public void run() {
                    try {
                        HtmlPage page = wc.getPage("https://newheaven.nl/index.php?strWebValue=extra&strWebAction=shoutbox");
                        String[][] table = getTable(page);
                        // System.out.println(Arrays.deepToString(table));
                        if (!Arrays.deepEquals(lastTable, table)) {
                            showNotification(table[0], trayIcon);
                        }
                        lastTable = table;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 1000, 60 * 1000);
        }
    }

    private static String[][] getTable(HtmlPage page) {
        ArrayList<String[]> rows = new ArrayList<>();
        List<DomElement> tds = page.getElementsByTagName("td");
        for (int i = 0; i < tds.size() - 1; i++) {
            DomElement td = tds.get(i);
            if (td.hasAttribute("width") && td.hasAttribute("nowrap")) {
                HtmlElement img = td.getElementsByTagName("img").get(0);
                HtmlElement span = td.getElementsByTagName("span").get(0);
                DomElement td2 = tds.get(i + 1);
                rows.add(new String[]{
                        img.getAttribute("title"),
                        span.asNormalizedText(),
                        td2.asNormalizedText().trim()
                });
                i++;
            }
        }
        String[][] r = new String[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            r[i] = rows.get(i);
        }
        return r;
    }

    private static void showNotification(String[] row, TrayIcon trayIcon) {
        trayIcon.displayMessage(String.format("%s, %s", row[0], row[1]), row[2], TrayIcon.MessageType.INFO);
    }
}
