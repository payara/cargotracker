/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.tests.external.cargotracker.integration;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Michael Ranaldo <michael.ranaldo@payara.fish>
 * Tests that cargo tracker can be deployed and navigated using Selenium with the HtmlUnitDriver
 *
 */
@RunWith(Arquillian.class)
public class BrowsingTest {

    private static final Logger log = Logger.getLogger(BrowsingTest.class.getName());
    private HtmlUnitDriver driver;
    private FluentWait<WebDriver> wait;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(MavenImporter.class).loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class);
        return war;
    }

    @ArquillianResource
    private URL deploymentUrl;

    @Rule
    public TestName testName = new TestName();

    @Before
    @RunAsClient
    public void setUp() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        driver = new HtmlUnitDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.setJavascriptEnabled(true);
        wait = new FluentWait<WebDriver>(driver)
            .withTimeout(4, TimeUnit.SECONDS)
            .pollingEvery(100, TimeUnit.MILLISECONDS)
            .ignoring(NoSuchElementException.class);
    }

    @After
    public void tearDown() {
        driver.quit();
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void landingPageTest() {
        log.info("Checking landing page loads properly.");
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect landing page", "Cargo Tracker", driver.getTitle());
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void dashboardTest() {
        log.info("Checking dashboard page loads properly.");
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect page", "Cargo Tracker", driver.getTitle());
        driver.findElement(By.linkText("Administration Interface")).click();
        Assert.assertEquals("Incorrect page", "Cargo Dashboard", driver.getTitle());
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void trackTest() {
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect page", "Cargo Tracker", driver.getTitle());
        driver.findElement(By.linkText("Administration Interface")).click();
        Assert.assertEquals("Incorrect page", "Cargo Dashboard", driver.getTitle());
        driver.findElement(By.linkText("Book")).click();
        // Admin track page is currently broken, awaiting patches PAYARA-1895 and PAYARA-1897
    }

    /**
     * This creates a new cargo, but does not save it
     */
    @Test
    @RunAsClient
    @InSequence(4)
    public void createCargo() {
        log.info("Checking creation of cargo.");

        // Navigate to registration form page
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect page", "Cargo Tracker", driver.getTitle());
        driver.findElement(By.linkText("Administration Interface")).click();
        Assert.assertEquals("Incorrect page", "Cargo Dashboard", driver.getTitle());
        driver.findElement(By.linkText("Book")).click();
        Assert.assertEquals("Incorrect page", "Cargo Registration", driver.getTitle());

        // Begin completing form
        log.info("Beginning completion of form.");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("originForm:next")));

        // Select origin
        Select origin = new Select(driver.findElement(By.name("originForm:origin_input")));
        origin.selectByValue("USCHI");
        log.info("Selected origin.");
        driver.findElement(By.id("originForm:next")).click();
        Assert.assertEquals("Incorrect page", "Cargo Registration", driver.getTitle());
        wait.until(ExpectedConditions.elementToBeClickable(By.id("destinationForm:next")));

        // Select destination
        Select destination = new Select(driver.findElement(By.name("destinationForm:destination_input")));
        destination.selectByValue("JNTKO");
        log.info("Selected destination.");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("destinationForm:next")));
        driver.findElement(By.id("destinationForm:next")).click();
        Assert.assertEquals("Incorrect page", "Cargo Registration", driver.getTitle());

        // Select dates
        for (int i = 0; i < 3; i++) {
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Next")));
            driver.findElement(By.linkText("Next")).click();
        }
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("18")));
        driver.findElement(By.linkText("18")).click();
        log.info("Selected dates.");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("dateForm:bookBtn")));
        driver.findElement(By.id("dateForm:bookBtn")).click();
        
        // Wait for the dashboard to load.
        log.info("Waiting for completion.");
        wait.until((driver) -> {
            return driver.findElement(By.id("mainDash")) != null;
        });

        Assert.assertEquals("Incorrect page", "Cargo Dashboard", driver.getTitle());
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void viewLiveMap() {
        log.info("Checking live map page loads correctly.");
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect page", "Cargo Tracker", driver.getTitle());
        driver.findElement(By.linkText("Administration Interface")).click();
        Assert.assertEquals("Incorrect page", "Cargo Dashboard", driver.getTitle());
        driver.findElement(By.id("navbar:live")).click();
        Assert.assertEquals("Incorrect page", "Live Map", driver.getTitle());
    }

    @Test
    @RunAsClient
    @InSequence(6)
    public void viewAboutPage() {
        log.info("Checking about page loads correctly.");
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect page", "Cargo Tracker", driver.getTitle());
        driver.findElement(By.linkText("Administration Interface")).click();
        Assert.assertEquals("Incorrect page", "Cargo Dashboard", driver.getTitle());
        driver.findElement(By.id("navbar:about")).click();
        Assert.assertEquals("Incorrect page", "Cargo Administration", driver.getTitle());
    }

    @Test
    @RunAsClient
    @InSequence(7)
    public void viewPublicTrackingPage() {
        log.info("Checking tracking page loads correctly.");
        driver.navigate().to(deploymentUrl);
        Assert.assertEquals("Incorrect page", "Cargo Tracker", driver.getTitle());
        driver.findElement(By.linkText("Public Tracking Interface")).click();
        Assert.assertEquals("Incorrect page", "Track Cargo", driver.getTitle());
        // Type 'AB' into search box
        driver.findElement(By.id("trackingForm:trackingId_input")).sendKeys("AB");

        // Click first suggestion
        driver.findElement(By.xpath("//div[@id='trackingForm:trackingId_panel']/ul/li")).click();
        log.info("Selected cargo.");

        // Check the field value isn't empty
        Assert.assertFalse("Text not entered",
                driver.findElement(By.id("trackingForm:trackingId_input")).getAttribute("value").isEmpty());

        // Check the submit button exists, and click it if it does.
        Assert.assertTrue("Track! button not present",
                driver.findElement(By.id("trackingForm:trackEnthusiasm")) != null);
        driver.findElement(By.id("trackingForm:trackingId_input")).submit();
        
        // Wait for the page to load.
        wait.until((driver) -> {
            return driver.findElement(By.id("trackingData")) != null;
        });

        // Check a valid cargo was found
        log.info("Loaded data for cargo.");
        Assert.assertTrue("Wrong cargo", driver.findElement(By.id("currentLocation")).getText()
                .matches("Cargo \\w+ is currently .+"));
    }
}
