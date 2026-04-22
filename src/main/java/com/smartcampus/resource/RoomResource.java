package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2 — Room Resource
 * Manages CRUD operations on the /api/v1/rooms collection.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ✔ Added for REST Location header support
    @Context
    private UriInfo uriInfo;

    // ── GET /rooms ────────────────────────────────────────────────────────────

    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // ── POST /rooms ───────────────────────────────────────────────────────────

    @POST
    public Response createRoom(Room room) {

        // Validate required fields
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                                   "message", "Room 'id' field is required"))
                    .build();
        }

        if (room.getName() == null || room.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                                   "message", "Room 'name' field is required"))
                    .build();
        }

        // Check duplicate
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict",
                                   "message", "Room with ID '" + room.getId() + "' already exists"))
                    .build();
        }

        // Ensure sensor list not null
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        // ✔ UPDATED RESPONSE (LOCATION HEADER ADDED)
        return Response.status(Response.Status.CREATED)
                .location(uriInfo.getAbsolutePathBuilder()
                        .path(room.getId())
                        .build())
                .entity(room)
                .build();
    }

    // ── GET /rooms/{roomId} ───────────────────────────────────────────────────

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Room not found: " + roomId))
                    .build();
        }

        return Response.ok(room).build();
    }

    // ── DELETE /rooms/{roomId} ────────────────────────────────────────────────

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Room not found: " + roomId))
                    .build();
        }

        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted. It has "
                            + room.getSensorIds().size()
                            + " active sensor(s) assigned."
            );
        }

        store.getRooms().remove(roomId);

        return Response.noContent().build();
    }
}