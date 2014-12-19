package com.hubspot.baragon.service.resources;

import java.util.Collection;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.UpstreamInfo;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public StateResource(BaragonStateDatastore stateDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore) {
    this.stateDatastore = stateDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @GET
  public Collection<BaragonServiceState> getServices() {
    return stateDatastore.getGlobalState();
  }

  @GET
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> getService(@PathParam("serviceId") String serviceId) throws Exception {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new BaragonServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreamsMap(serviceId).values()));
  }

  @DELETE
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> deleteService(@PathParam("serviceId") String serviceId) throws Exception {
    final Optional<BaragonService> maybeServiceInfo = stateDatastore.getService(serviceId);

    if (!maybeServiceInfo.isPresent()) {
      return Optional.absent();
    }

    stateDatastore.removeService(serviceId);
    for (String loadBalancerGroup : maybeServiceInfo.get().getLoadBalancerGroups()) {
      loadBalancerDatastore.clearBasePath(loadBalancerGroup, maybeServiceInfo.get().getServiceBasePath());
    }

    return Optional.of(new BaragonServiceState(maybeServiceInfo.get(), stateDatastore.getUpstreamsMap(serviceId).values()));
  }
}
