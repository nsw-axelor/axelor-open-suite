package com.axelor.apps.sale.db.repo;

import com.axelor.apps.base.service.administration.GeneralService;
import com.axelor.apps.sale.db.ISaleOrder;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.service.SaleOrderService;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;

public class SaleOrderManagementRepository extends SaleOrderRepository {

	@Override
	public SaleOrder copy(SaleOrder entity, boolean deep) {

		SaleOrder copy = super.copy(entity, deep);

		copy.setStatusSelect(ISaleOrder.STATUS_DRAFT);
		copy.setSaleOrderSeq(null);
		copy.clearBatchSet();
		copy.setImportId(null);
		copy.setCreationDate(GeneralService.getTodayDate());
		copy.setConfirmationDate(null);
		copy.setConfirmedByUser(null);
		copy.setOrderDate(null);
		copy.setOrderNumber(null);
		copy.setVersionNumber(1);

		return copy;
	}
	
	@Override
	public SaleOrder save(SaleOrder saleOrder) {
		try {
			saleOrder = super.save(saleOrder);
			Beans.get(SaleOrderService.class).setDraftSequence(saleOrder);

			return saleOrder;
		} catch (Exception e) {
			JPA.em().getTransaction().rollback();
			e.printStackTrace();
		}
		return null;
	}
}
