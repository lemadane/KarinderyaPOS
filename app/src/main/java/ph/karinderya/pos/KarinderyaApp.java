package ph.karinderya.pos;

import android.app.Application;
import android.content.Context;

import ph.karinderya.pos.data.CartManager;
import ph.karinderya.pos.data.SessionManager;
import ph.karinderya.pos.data.SettingsStore;
import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.repo.BackupService;
import ph.karinderya.pos.data.repo.DiscountRepository;
import ph.karinderya.pos.data.repo.MenuRepository;
import ph.karinderya.pos.data.repo.OrderRepository;
import ph.karinderya.pos.data.repo.ReportRepository;
import ph.karinderya.pos.data.repo.UserRepository;
import ph.karinderya.pos.trial.TrialGuard;
import ph.karinderya.pos.util.ImageStore;

/** Application-wide service locator (no DI framework needed at this size). */
public class KarinderyaApp extends Application {

     UserRepository users;
     MenuRepository menu;
     OrderRepository orders;
     ReportRepository reports;
     BackupService backup;
     DiscountRepository discounts;
     ImageStore images;
     SessionManager session;
     CartManager cart;
     SettingsStore settings;
     TrialGuard trial;

    public static KarinderyaApp get(Context context) {
        return (KarinderyaApp) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DbHelper db = new DbHelper(this);
        images = new ImageStore(this);
        users = new UserRepository(db);
        menu = new MenuRepository(db);
        orders = new OrderRepository(db);
        reports = new ReportRepository(db);
        discounts = new DiscountRepository(db);
        backup = new BackupService(db, menu, orders, images);
        session = new SessionManager(this, users);
        cart = new CartManager(this);
        settings = new SettingsStore(this);
        trial = new TrialGuard(this);
    }

    public UserRepository users() {
        return users;
    }

    public MenuRepository menu() {
        return menu;
    }

    public OrderRepository orders() {
        return orders;
    }

    public ReportRepository reports() {
        return reports;
    }

    public BackupService backup() {
        return backup;
    }

    public DiscountRepository discounts() {
        return discounts;
    }

    public ImageStore images() {
        return images;
    }

    public SessionManager session() {
        return session;
    }

    public CartManager cart() {
        return cart;
    }

    public SettingsStore settings() {
        return settings;
    }

    public TrialGuard trial() {
        return trial;
    }
}
