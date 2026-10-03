package com.oms.web.servlet;

import com.oms.model.Shipment;
import com.oms.service.ServiceException;
import com.oms.service.ShipmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/shipments/confirm")
@MultipartConfig(maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class ShipmentConfirmationServlet extends BaseServlet {

    private final ShipmentService shipmentService = new ShipmentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long shipmentId = longParam(req, "id");
        try {
            Shipment shipment = shipmentService.get(shipmentId);
            req.setAttribute("shipment", shipment);
            req.setAttribute("success", req.getParameter("success"));
            render(req, resp, "shipments/confirm");
        } catch (ShipmentService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long shipmentId = longParam(req, "id");
        String outcome = param(req, "outcome");
        try {
            if ("SUCCESS".equalsIgnoreCase(outcome)) {
                byte[] proofImage = readUpload(req.getPart("proofImage"));
                shipmentService.confirmSuccess(shipmentId, param(req, "recipientName"), proofImage, currentUser(req));
                flash(req, "Đã xác nhận giao hàng thành công.");
            } else if ("FAILED".equalsIgnoreCase(outcome)) {
                shipmentService.confirmFailure(shipmentId, param(req, "failureReason"), currentUser(req));
                flash(req, "Đã ghi nhận giao không thành công và hàng quay về kho.");
            } else {
                throw new ServiceException("Kết quả giao hàng không hợp lệ.");
            }
            redirect(req, resp, "/shipments/confirm?id=" + shipmentId);
        } catch (ShipmentService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            try {
                req.setAttribute("shipment", shipmentService.get(shipmentId));
                render(req, resp, "shipments/confirm");
            } catch (ShipmentService.NotFoundException notFound) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (SQLException sqlException) {
                throw new ServletException(sqlException);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
