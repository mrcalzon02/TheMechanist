package mechanist.modapi.examples;

import mechanist.modapi.ModIntegrationHook;
import mechanist.modapi.NavigationVector;
import mechanist.modapi.SectorCoordinates;
import mechanist.modapi.SectorInstance;
import mechanist.modapi.SimulationContext;
import mechanist.modapi.SimulationEvent;

/** Sector Editor example: fluctuating gravity and repeating spatial anomaly navigation drift. */
public final class AnomalousCosmicSectorMod implements ModIntegrationHook {
    public static final String MOD_ID = "mechanist.example.anomalous_cosmic_sector";
    public static final String SECTOR_ID = "sector.null-tide-recurrence";

    @Override public String modId() { return MOD_ID; }

    @Override public void onRegister(SimulationContext context) {
        SectorInstance sector = new SectorInstance(SECTOR_ID, "Null-Tide Recurrence", new SectorCoordinates(41, -9, 7));
        sector.addEnvironmentalHazard("fluctuating-gravity-field");
        sector.addEnvironmentalHazard("repeating-spatial-anomaly");
        sector.setFactionControl("neutral-cartographer-guild", 45);
        sector.setFactionControl("void-salvage-compacts", 31);
        sector.setAttribute("gravityFluxAmplitude", 0.38d);
        sector.setAttribute("anomalyPeriodTicks", 12L);
        sector.setNavigationVector(new NavigationVector(1.0, 0.0, 0.15));
        context.registerSector(sector);
        context.audit(MOD_ID, "registered anomalous cosmic sector data");
    }

    @Override public void onSectorEnter(SimulationContext context, SectorInstance sector) {
        if (!SECTOR_ID.equals(sector.id())) return;
        NavigationVector oldVector = sector.navigationVector();
        NavigationVector shifted = oldVector.add(new NavigationVector(0.18, -0.07, 0.05)).clampMagnitude(2.0);
        sector.setNavigationVector(shifted);
        sector.setAttribute("lastEntryTick", context.tick());
        context.emit(new SimulationEvent.NavigationVectorChanged(sector.id(), oldVector, shifted, "entry shear from null-tide gravity field"));
    }

    @Override public void onSectorTick(SimulationContext context, SectorInstance sector) {
        if (!SECTOR_ID.equals(sector.id())) return;
        long period = 12L;
        Object configured = sector.attributes().get("anomalyPeriodTicks");
        if (configured instanceof Number number) period = Math.max(1L, number.longValue());
        if (context.tick() % period != 0L) return;
        double wave = Math.sin(context.tick() / 3.0d) * 0.11d;
        NavigationVector oldVector = sector.navigationVector();
        NavigationVector shifted = oldVector.add(new NavigationVector(wave, -wave / 2.0d, wave / 4.0d)).clampMagnitude(2.25d);
        sector.setNavigationVector(shifted);
        sector.setAttribute("lastAnomalyPulseTick", context.tick());
        context.emit(new SimulationEvent.NavigationVectorChanged(sector.id(), oldVector, shifted, "repeating anomaly pulse altered fleet navigation vector"));
    }
}
