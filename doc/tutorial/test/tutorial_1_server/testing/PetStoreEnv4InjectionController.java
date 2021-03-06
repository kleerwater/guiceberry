package tutorial_1_server.testing;

import com.google.guiceberry.GuiceBerryEnvMain;
import com.google.guiceberry.GuiceBerryModule;
import com.google.guiceberry.TestId;
import com.google.guiceberry.TestScoped;
import com.google.guiceberry.controllable.IcMaster;
import com.google.guiceberry.controllable.StaticMapInjectionController;
import com.google.guiceberry.controllable.TestIdServerModule;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import tutorial_1_server.prod.PetStoreServer;
import tutorial_1_server.prod.Pet;
import tutorial_1_server.prod.Featured;

public final class PetStoreEnv4InjectionController extends AbstractModule {
  
  @Provides @Singleton
  @PortNumber int getPortNumber() {
    return FreePortFinder.findFreePort();
  }
  
  @Provides @TestScoped
  WebDriver getWebDriver(@PortNumber int portNumber, TestId testId) {
    WebDriver driver = new HtmlUnitDriver();
    driver.get("http://localhost:" + portNumber);
    driver.manage().addCookie(new Cookie(TestId.COOKIE_NAME, testId.toString()));
    return driver;
  }
  
  @Provides
  @Singleton
  PetStoreServer buildPetStoreServer(@PortNumber int portNumber) {
    PetStoreServer result = new PetStoreServer(portNumber) {
      @Override
      protected Module getPetStoreModule() {
        // !!! HERE !!!
        return icMaster.buildServerModule(
            new TestIdServerModule(),
            super.getPetStoreModule());
      }
    };
    return result;
  }
  
  private IcMaster icMaster;
  
  @Override
  protected void configure() {
    install(new GuiceBerryModule());
    bind(GuiceBerryEnvMain.class).to(PetStoreServerStarter.class);
    // !!!! HERE !!!!
    icMaster = new IcMaster()
      .thatControls(StaticMapInjectionController.strategy(),
         Key.get(Pet.class, Featured.class));
    install(icMaster.buildClientModule());
  }
  
  private static final class PetStoreServerStarter implements GuiceBerryEnvMain {
    
    @Inject
    private PetStoreServer petStoreServer;
    
    public void run() {
      // Starting a server should never be done in a @Provides method 
      // (or inside Provider's get).
      petStoreServer.start();
    }
  }
}

