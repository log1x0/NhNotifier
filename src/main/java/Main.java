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
    public static class Notification {
        public String date;
        public String user;
        public String text;

        public Notification(String date, String user, String text) {
            if (date == null || user == null || text == null) {
                throw new IllegalArgumentException();
            }
            this.date = date;
            this.user = user;
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Notification that = (Notification) o;
            return date.equals(that.date) && user.equals(that.user) && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, user, text);
        }
    }

    public static void main(String[] args) throws AWTException {
        if (args.length != 2 || args[0].isBlank() || args[1].isBlank()) {
            System.out.println("Please enter (the cookie) uid and pass next time.");
            return;
        }

        final SystemTray tray = SystemTray.getSystemTray();
        final Image image = Toolkit.getDefaultToolkit().createImage(Main.class.getResource("favicon.jpg"));
        final TrayIcon trayIcon = new TrayIcon(image, "Tray NH");
        final PopupMenu popupMenu = new PopupMenu();
        final MenuItem allItem = new MenuItem("Show last 10");
        final MenuItem exitItem = new MenuItem("Exit");
        popupMenu.add(allItem);
        popupMenu.add(exitItem);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("System tray NH");
        trayIcon.setPopupMenu(popupMenu);
        tray.add(trayIcon);

        try (WebClient wc = new WebClient(BrowserVersion.FIREFOX)) {
            wc.getOptions().setThrowExceptionOnScriptError(false);
            wc.getOptions().setCssEnabled(false);
            wc.getCookieManager().addCookie(new Cookie("newheaven.nl", "uid", args[0]));
            wc.getCookieManager().addCookie(new Cookie("newheaven.nl", "pass", args[1]));
            new Timer().schedule(new TimerTask() {
                Notification lastNotification = new Notification("", "", "");

                @Override
                public void run() {
                    try {
                        HtmlPage page = wc.getPage("https://newheaven.nl/index.php?strWebValue=extra&strWebAction=shoutbox");
                        ArrayList<Notification> notifications = getTable(page);
                        if (!notifications.isEmpty() && !notifications.get(0).equals(lastNotification)) {
                            showNotification(notifications.get(0), trayIcon);
                            lastNotification = notifications.get(0);
                        }
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                        System.exit(0);
                    }
                }
            }, 1000, 30 * 1000);

            allItem.addActionListener(e -> {
                try {
                    HtmlPage page = wc.getPage("https://newheaven.nl/index.php?strWebValue=extra&strWebAction=shoutbox");
                    ArrayList<Notification> notifications = getTable(page);
                    ArrayList<Notification> n2 = new ArrayList<>();
                    for (int i = 0; i < 10 && i < notifications.size(); i++) {
                        n2.add(notifications.get(i));
                    }
                    Collections.reverse(n2);
                    n2.forEach(n -> showNotification(n, trayIcon));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            });
        }

        exitItem.addActionListener(e -> System.exit(0));
    }

    private static ArrayList<Notification> getTable(HtmlPage page) {
        ArrayList<Notification> notifications = new ArrayList<>();
        List<DomElement> tds = page.getElementsByTagName("td");
        for (int i = 0; i < tds.size() - 1; i++) {
            try {
                DomElement td = tds.get(i);
                if (td.hasAttribute("width") && td.hasAttribute("nowrap")) {
                    HtmlElement img = td.getElementsByTagName("img").get(0);
                    HtmlElement span = td.getElementsByTagName("span").get(0);
                    DomElement td2 = tds.get(i + 1);
                    notifications.add(new Notification(
                            img.getAttribute("title"),
                            span.asNormalizedText(),
                            td2.asNormalizedText().trim()
                    ));
                    i++;
                }
            } catch (Exception ignore) {
            }
        }
        return notifications;
    }

    private static void showNotification(Notification n, TrayIcon trayIcon) {
        trayIcon.displayMessage(String.format("%s, %s", n.date, n.user), n.text, TrayIcon.MessageType.INFO);
    }
}
