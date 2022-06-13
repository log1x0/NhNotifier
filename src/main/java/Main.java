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
    private final WebClient wc;
    private final TrayIcon trayIcon;
    private final Timer timer;
    private final String sbUrl = "https://newheaven.nl/index.php?strWebValue=extra&strWebAction=shoutbox";
    private Notification lastNotification = new Notification("", "", "");

    public Main(String uid, String pass) throws AWTException {
        wc = new WebClient(BrowserVersion.FIREFOX);
        wc.getOptions().setThrowExceptionOnScriptError(false);
        wc.getOptions().setCssEnabled(false);
        wc.getCookieManager().addCookie(new Cookie("newheaven.nl", "uid", uid));
        wc.getCookieManager().addCookie(new Cookie("newheaven.nl", "pass", pass));

        final PopupMenu popupMenu = new PopupMenu();
        final MenuItem allItem = new MenuItem("Show last 10");
        final MenuItem exitItem = new MenuItem("Exit");
        popupMenu.add(allItem);
        popupMenu.add(exitItem);

        final Image image = Toolkit.getDefaultToolkit().createImage(Main.class.getResource("favicon.jpg"));
        trayIcon = new TrayIcon(image, "Tray NH");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("System tray NH");
        trayIcon.setPopupMenu(popupMenu);

        final SystemTray tray = SystemTray.getSystemTray();
        tray.add(trayIcon);

        allItem.addActionListener(e -> lasts());
        exitItem.addActionListener(e -> System.exit(0));

        timer = new Timer();
        scheduleTimer();
    }

    private synchronized void lasts() {
        try {
            HtmlPage page = wc.getPage(sbUrl);
            ArrayList<Notification> table = getTable(page);
            ArrayList<Notification> temp = new ArrayList<>();
            for (int i = 0; i < 10 && i < table.size(); i++) {
                temp.add(table.get(i));
            }
            Collections.reverse(temp);
            temp.forEach(this::showNotification);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void scheduleTimer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runTimer();
            }
        }, 1000, 30 * 1000);
    }

    private synchronized void runTimer() {
        try {
            HtmlPage page = wc.getPage(sbUrl);
            ArrayList<Notification> table = getTable(page);
            if (!table.isEmpty() && !table.get(0).equals(lastNotification)) {
                showNotification(table.get(0));
                lastNotification = table.get(0);
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.exit(0);
        }
    }

    private ArrayList<Notification> getTable(HtmlPage page) {
        ArrayList<Notification> table = new ArrayList<>();
        List<DomElement> tds = page.getElementsByTagName("td");
        for (int i = 0; i < tds.size() - 1; i++) {
            try {
                DomElement td = tds.get(i);
                if (td.hasAttribute("width") && td.hasAttribute("nowrap")) {
                    HtmlElement img = td.getElementsByTagName("img").get(0);
                    HtmlElement span = td.getElementsByTagName("span").get(0);
                    DomElement td2 = tds.get(i + 1);
                    table.add(new Notification(
                            img.getAttribute("title"),
                            span.asNormalizedText(),
                            td2.asNormalizedText().trim()
                    ));
                    i++;
                }
            } catch (Exception ignore) {
            }
        }
        return table;
    }

    private void showNotification(Notification n) {
        trayIcon.displayMessage(String.format("%s, %s", n.date, n.user), n.text, TrayIcon.MessageType.INFO);
    }

    public record Notification(String date, String user, String text) {
    }

    public static void main(String[] args) throws AWTException {
        if (args.length != 2) {
            System.out.println("Please enter (the cookie) uid and pass next time.");
            return;
        }
        String u = args[0];
        String p = args[1];
        new Main(u, p);
    }
}
