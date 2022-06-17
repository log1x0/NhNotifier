import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

public class Main {
    private final WebClient wc;
    private final TrayIcon trayIcon;
    private final Timer timer;
    private final String sbUrl = "https://newheaven.nl/index.php?strWebValue=extra&strWebAction=shoutbox";
    private Notification lastNotification = new Notification("", "", "");

    public Main(String uid, String pass) throws AWTException {
        wc = new WebClient(BrowserVersion.FIREFOX);
        wc.getOptions().setJavaScriptEnabled(false);
        wc.getOptions().setCssEnabled(false);
        String nhDomain = "newheaven.nl";
        wc.getCookieManager().addCookie(new Cookie(nhDomain, "uid", uid));
        wc.getCookieManager().addCookie(new Cookie(nhDomain, "pass", pass));

        final PopupMenu popupMenu = new PopupMenu();
        final Menu csvItem = new Menu("Create csv file");
        final MenuItem allItem = new MenuItem("Show last 10");
        final MenuItem exitItem = new MenuItem("Exit");
        final MenuItem p1Item = new MenuItem("1 Page");
        final MenuItem p2Item = new MenuItem("2 Pages");
        final MenuItem p3Item = new MenuItem("3 Pages");
        final MenuItem p4Item = new MenuItem("4 Pages");
        csvItem.add(p1Item);
        csvItem.add(p2Item);
        csvItem.add(p3Item);
        csvItem.add(p4Item);
        popupMenu.add(csvItem);
        popupMenu.add(allItem);
        popupMenu.add(exitItem);

        final Image image = Toolkit.getDefaultToolkit().createImage(Main.class.getResource("favicon.jpg"));
        trayIcon = new TrayIcon(image, "Tray NH");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("System tray NH");
        trayIcon.setPopupMenu(popupMenu);

        final SystemTray tray = SystemTray.getSystemTray();
        tray.add(trayIcon);

        p1Item.addActionListener(e -> csvFile(1));
        p2Item.addActionListener(e -> csvFile(2));
        p3Item.addActionListener(e -> csvFile(3));
        p4Item.addActionListener(e -> csvFile(4));
        allItem.addActionListener(e -> lasts());
        exitItem.addActionListener(e -> System.exit(0));

        try {
            HtmlPage page = wc.getPage(sbUrl);
            String sbLink = "index.php?strWebValue=extra&strWebAction=shoutbox&page=1";
            page.getAnchorByHref(sbLink);
        } catch (Exception e) {
            System.out.println("Please enter the correct uid and pass next time.");
            System.exit(0);
        }

        timer = new Timer();
        scheduleTimer();
    }

    private synchronized void csvFile(int pages) {
        try (WebClient wc2 = new WebClient(BrowserVersion.FIREFOX)) {
            wc2.getCookieManager().addCookie(wc.getCookieManager().getCookie("uid"));
            wc2.getCookieManager().addCookie(wc.getCookieManager().getCookie("pass"));
            wc2.getOptions().setJavaScriptEnabled(false);
            wc2.getOptions().setCssEnabled(false);
            ArrayList<ItemCsv> rows = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= pages; pageNumber++) {
                String csvPrefix = "https://newheaven.nl/index.php?strWebValue=torrent&strWebAction=list&page=";
                HtmlPage page = wc2.getPage(csvPrefix + pageNumber);
                List<DomElement> as = page.getByXPath("//table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td[2]/table/tbody/tr[2]/td/a").stream().map(o -> (DomElement) o).toList();
                Pattern pat = Pattern.compile("'([^']+)'");
                for (DomElement a : as) {
                    String name = a.asNormalizedText();
                    String mouse = a.getAttribute("onmouseover");
                    List<String> rs = pat.matcher(mouse).results().map(mr -> mr.group(1)).toList();
                    if (!rs.isEmpty()) {
                        System.out.println("rs = " + rs);
                        int comments = Integer.parseInt(rs.get(2));
                        int seeder = Integer.parseInt(rs.get(3));
                        int lecher = Integer.parseInt(rs.get(4));
                        String size = rs.get(5);
                        int snatched = Integer.parseInt(rs.get(6));
                        String from = rs.get(7);
                        String[] sizeSplit = size.split(" ");
                        String first = sizeSplit[0].replace(",", "");
                        double sizeNormal = switch (sizeSplit[1]) {
                            case "MB" -> Double.parseDouble(first) * 1000;
                            case "GB" -> Double.parseDouble(first) * 1000000;
                            default -> Double.parseDouble(first);
                        };
                        rows.add(new ItemCsv(name, from, comments, seeder, lecher, snatched, sizeNormal));
                    }
                }
            }
            double maxSeeder = rows.stream().map(ItemCsv::seeder).max(Integer::compare).orElse(-1);
            double maxLecher = rows.stream().map(ItemCsv::lecher).max(Integer::compare).orElse(-1);
            double maxSnatched = rows.stream().map(ItemCsv::snatched).max(Integer::compare).orElse(-1);
            double maxSize = rows.stream().map(ItemCsv::sizeNormal).max(Double::compare).orElse(-1.0);
            String[] headers = {
                    "Index", "Name", "From", "Comments", "Seeder", "Lecher", "Snatched", "Size MB", "Score"
            };
            try (CSVPrinter p = new CSVPrinter(new FileWriter("nh-" + System.currentTimeMillis() + ".csv"), CSVFormat.Builder.create(CSVFormat.DEFAULT).setQuoteMode(QuoteMode.ALL).setHeader(headers).build())) {
                for (int i = 0; i < rows.size(); i++) {
                    ItemCsv r = rows.get(i);
                    double score = (r.seeder / maxSeeder + r.lecher / maxLecher + r.snatched / maxSnatched + r.sizeNormal / maxSize) / 4.0;
                    p.printRecord(i + 1, r.name, r.from, r.comments, r.seeder, r.lecher, r.snatched, (float) (r.sizeNormal / 1000.0), (float) score);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
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

    public record ItemCsv(String name, String from, int comments, int seeder, int lecher, int snatched,
                          double sizeNormal) {
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
