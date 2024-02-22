package net.java.cargotracker.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.lang3.time.DateUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.importer.MavenImporter;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.java.cargotracker.domain.model.cargo.Cargo;
import net.java.cargotracker.domain.model.cargo.Delivery;
import net.java.cargotracker.domain.model.cargo.Itinerary;
import net.java.cargotracker.domain.model.cargo.RoutingStatus;
import net.java.cargotracker.domain.model.cargo.TrackingId;
import net.java.cargotracker.domain.model.cargo.TransportStatus;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.model.location.SampleLocations;
import net.java.cargotracker.domain.model.location.UnLocode;
import net.java.cargotracker.domain.model.voyage.Voyage;

/**
 * Application layer integration test covering a number of otherwise fairly
 * trivial components that largely do not warrant their own tests.
 *
 * @author Reza
 */
@RunWith(Arquillian.class)
public class BookingServiceTest {

    @Inject
    private BookingService bookingService;
    @PersistenceContext
    private EntityManager entityManager;

    private static TrackingId trackingId;
    private static List<Itinerary> candidates;
    private static Date deadline;
    private static Itinerary assigned;

    @Deployment
    public static WebArchive createDeployment() {
        String payaraVersion = System.getProperty("payara.version.major");
        String webXml = "webPayara7.xml";

        if (payaraVersion != null && payaraVersion.equals("4")) {
            webXml = "webPayara4.xml";
        } else if (payaraVersion != null && payaraVersion.equals("5")) {
            webXml = "webPayara5.xml";
        } else if (payaraVersion != null && payaraVersion.equals("6")) {
            webXml = "webPayara6.xml";
        }

        WebArchive war = ShrinkWrap.create(MavenImporter.class)
                .loadPomFromFile("pom.xml").importBuildOutput()
                .as(WebArchive.class).setWebXML(new File("src/test/resources/" + webXml));
        
        return war;
    }

    @Test
    @InSequence(1)
    public void testRegisterNew() {
        UnLocode fromUnlocode = new UnLocode("USCHI");
        UnLocode toUnlocode = new UnLocode("SESTO");

        deadline = new Date();
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(deadline);
        calendar.add(Calendar.MONTH, 6); // Six months ahead.
        deadline.setTime(calendar.getTime().getTime());

        trackingId = bookingService.bookNewCargo(fromUnlocode, toUnlocode,
                deadline);

        Cargo cargo = entityManager
                .createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
        assertEquals(SampleLocations.STOCKHOLM, cargo.getRouteSpecification()
                .getDestination());
        assertTrue(DateUtils.isSameDay(deadline, cargo.getRouteSpecification()
                .getArrivalDeadline()));
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery()
                .getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery()
                .getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery()
                .getEstimatedTimeOfArrival());
        assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery()
                .getNextExpectedActivity());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.NOT_ROUTED, cargo.getDelivery()
                .getRoutingStatus());
        assertEquals(Itinerary.EMPTY_ITINERARY, cargo.getItinerary());
    }

    @Test
    @InSequence(2)
    public void testRouteCandidates() {
        candidates = bookingService.requestPossibleRoutesForCargo(trackingId);

        assertFalse(candidates.isEmpty());
    }

    @Test
    @InSequence(3)
    public void testAssignRoute() {
        assigned = candidates.get(new Random().nextInt(candidates
                .size()));

        bookingService.assignCargoToRoute(assigned, trackingId);

        Cargo cargo = entityManager
                .createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(assigned, cargo.getItinerary());
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery()
                .getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery()
                .getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertTrue(cargo.getDelivery().getEstimatedTimeOfArrival()
                .before(deadline));
        assertEquals(HandlingEvent.Type.RECEIVE, cargo.getDelivery()
                .getNextExpectedActivity().getType());
        assertEquals(SampleLocations.CHICAGO, cargo.getDelivery()
                .getNextExpectedActivity().getLocation());
        assertEquals(null, cargo.getDelivery().getNextExpectedActivity()
                .getVoyage());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.ROUTED, cargo.getDelivery()
                .getRoutingStatus());
    }

    @Test
    @InSequence(4)
    public void testChangeDestination() {
        bookingService.changeDestination(trackingId, new UnLocode("FIHEL"));

        Cargo cargo = entityManager
                .createNamedQuery("Cargo.findByTrackingId", Cargo.class)
                .setParameter("trackingId", trackingId).getSingleResult();

        assertEquals(SampleLocations.CHICAGO, cargo.getOrigin());
        assertEquals(SampleLocations.HELSINKI, cargo.getRouteSpecification()
                .getDestination());
        assertTrue(DateUtils.isSameDay(deadline, cargo.getRouteSpecification()
                .getArrivalDeadline()));
        assertEquals(assigned, cargo.getItinerary());
        assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery()
                .getTransportStatus());
        assertEquals(Location.UNKNOWN, cargo.getDelivery()
                .getLastKnownLocation());
        assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
        assertFalse(cargo.getDelivery().isMisdirected());
        assertEquals(Delivery.ETA_UNKOWN, cargo.getDelivery()
                .getEstimatedTimeOfArrival());
        assertEquals(Delivery.NO_ACTIVITY, cargo.getDelivery()
                .getNextExpectedActivity());
        assertFalse(cargo.getDelivery().isUnloadedAtDestination());
        assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery()
                .getRoutingStatus());
    }
}
