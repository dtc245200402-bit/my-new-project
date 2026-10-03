<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Xác nhận giao hàng"/>
<%@ include file="/WEB-INF/views/layout/app-top.jspf" %>

        <div class="page-header">
          <div>
            <p class="breadcrumb">Phiếu xuất &amp; giao hàng</p>
            <h1>Xác nhận giao hàng</h1>
          </div>
        </div>

        <c:if test="${not empty error}">
          <div class="alert alert--danger" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <section class="card">
          <div class="card__header">
            <h2>Phiếu <c:out value="${shipment.code}"/></h2>
          </div>
          <div class="card__body form-grid">
            <div class="form-field">
              <span class="form-label">Kho</span>
              <div><c:out value="${shipment.warehouseName}"/></div>
            </div>
            <div class="form-field">
              <span class="form-label">Đại lý</span>
              <div><c:out value="${shipment.customerName}"/></div>
            </div>
            <div class="form-field">
              <span class="form-label">Trạng thái</span>
              <div><c:out value="${shipment.status.label}"/></div>
            </div>
          </div>
        </section>

        <c:if test="${shipment.readyForConfirmation}">
          <section class="card">
            <div class="card__header">
              <h2>Kết quả giao hàng</h2>
              <p>Thời điểm giao được hệ thống ghi nhận theo giờ server khi xác nhận.</p>
            </div>
            <div class="card__body">
              <form action="${ctx}/shipments/confirm" method="post" enctype="multipart/form-data" class="form-grid" novalidate>
                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                <input type="hidden" name="id" value="<c:out value='${shipment.id}'/>">

                <fieldset class="form-field form-field--full">
                  <legend class="form-label">Kết quả <span class="required">*</span></legend>
                  <label class="choice">
                    <input type="radio" name="outcome" value="SUCCESS" checked>
                    <span class="choice__title">Giao thành công</span>
                  </label>
                  <label class="choice">
                    <input type="radio" name="outcome" value="FAILED">
                    <span class="choice__title">Giao không thành công</span>
                  </label>
                </fieldset>

                <div class="form-field">
                  <label class="form-label" for="recipientName">Người nhận</label>
                  <input class="input" id="recipientName" name="recipientName" maxlength="200" placeholder="Họ tên người nhận">
                </div>

                <div class="form-field">
                  <label class="form-label" for="proofImage">Ảnh biên bản có chữ ký</label>
                  <input class="input" id="proofImage" name="proofImage" type="file" accept="image/png,image/jpeg">
                  <p class="form-hint">Bắt buộc khi chọn giao thành công. JPG hoặc PNG, tối đa 2MB.</p>
                </div>

                <div class="form-field form-field--full">
                  <label class="form-label" for="failureReason">Lý do giao không thành công</label>
                  <textarea class="textarea" id="failureReason" name="failureReason" maxlength="500"
                            placeholder="Bắt buộc khi giao không thành công"></textarea>
                </div>

                <div class="form-actions form-field--full">
                  <button class="btn btn--primary" type="submit">Xác nhận giao hàng</button>
                </div>
              </form>
            </div>
          </section>
        </c:if>

        <c:if test="${shipment.status.code == 'DELIVERED'}">
          <section class="card">
            <div class="card__header"><h2>Chứng từ giao hàng</h2></div>
            <div class="card__body">
              <p>Người nhận: <strong><c:out value="${shipment.recipientName}"/></strong></p>
              <p>Thời điểm: <c:out value="${shipment.deliveredAt}"/></p>
              <c:if test="${not empty shipment.proofImagePath}">
                <img class="product-image" src="${ctx}/files/<c:out value='${shipment.proofImagePath}'/>"
                     alt="Ảnh biên bản giao hàng" width="400">
              </c:if>
              <p>Đủ điều kiện phát hành hoá đơn: <strong><c:out value="${shipment.invoiceEligible}"/></strong></p>
            </div>
          </section>
        </c:if>

        <c:if test="${shipment.status.code == 'FAILED_RETURNED'}">
          <section class="card">
            <div class="card__header"><h2>Giao không thành công</h2></div>
            <div class="card__body">
              <p>Lý do: <strong><c:out value="${shipment.failureReason}"/></strong></p>
              <p>Thời điểm ghi nhận quay về kho: <c:out value="${shipment.returnedAt}"/></p>
              <p>Hệ thống đã tạo các movement <strong>SHIPMENT_RETURN</strong> cho hàng của phiếu.</p>
            </div>
          </section>
        </c:if>

<%@ include file="/WEB-INF/views/layout/app-bottom.jspf" %>
