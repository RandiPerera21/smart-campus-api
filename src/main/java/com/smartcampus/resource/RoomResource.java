package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2 - Room Management.
 * Manages /api/v1/rooms
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // -----------------------------------------------------------------------
    // GET /api/v1/rooms
    // Returns all rooms. Returning full objects reduces client-side lookups
    // but costs more bandwidth than returning IDs only.
    // -----------------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.rooms.values());
        return Response.ok(roomList).build();
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/rooms
    // Creates a new room. Returns 201 Created with the created room.
    // -----------------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorBody("Room id is required."))
                    .build();
        }
        if (DataStore.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorBody("Room with id '" + room.getId() + "' already exists."))
                    .build();
        }
        DataStore.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}
    // Fetches a single room by ID.
    // -----------------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        return Response.ok(room).build();
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}
    // Idempotent: deleting a non-existent room after first delete returns 404.
    // Business rule: cannot delete a room that still has sensors assigned.
    // Throws RoomNotEmptyException -> mapped to HTTP 409 Conflict.
    // -----------------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorBody("Room '" + roomId + "' not found."))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            // Blocked: sensors still assigned to this room
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted because it has " +
                    room.getSensorIds().size() + " sensor(s) still assigned to it."
            );
        }
        DataStore.rooms.remove(roomId);
        return Response.noContent().build(); // 204
    }

    // Helper: builds a simple error JSON map
    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}
