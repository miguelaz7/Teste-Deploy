package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import jakarta.persistence.*;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "trip_headsign")
    private String tripHeadsign;

    @Column(name = "direction_id")
    private String directionId;

    @Column(name = "shape_id")
    private String shapeId;

    // Usado pelo P6 (ControladorCalculoMatrizOD) para estimar o terminus da linha
    @Column(name = "last_stop_id")
    private String lastStopId;

    public Trip() {}

    public Trip(String tripId, String routeId, String serviceId,
                String tripHeadsign, String directionId, String shapeId) {
        this.tripId       = tripId;
        this.routeId      = routeId;
        this.serviceId    = serviceId;
        this.tripHeadsign = tripHeadsign;
        this.directionId  = directionId;
        this.shapeId      = shapeId;
    }

    public String getTripId()                     { return tripId; }
    public void setTripId(String tripId)          { this.tripId = tripId; }
    public String getRouteId()                    { return routeId; }
    public void setRouteId(String routeId)        { this.routeId = routeId; }
    public String getServiceId()                  { return serviceId; }
    public void setServiceId(String serviceId)    { this.serviceId = serviceId; }
    public String getTripHeadsign()               { return tripHeadsign; }
    public void setTripHeadsign(String t)         { this.tripHeadsign = t; }
    public String getDirectionId()                { return directionId; }
    public void setDirectionId(String directionId){ this.directionId = directionId; }
    public String getShapeId()                    { return shapeId; }
    public void setShapeId(String shapeId)        { this.shapeId = shapeId; }
    public String getLastStopId()                 { return lastStopId; }
    public void setLastStopId(String lastStopId)  { this.lastStopId = lastStopId; }
}
