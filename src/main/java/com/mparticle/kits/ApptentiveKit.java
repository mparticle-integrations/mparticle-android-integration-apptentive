package com.mparticle.kits;

import android.app.Application;
import android.content.Context;

import com.apptentive.android.sdk.Apptentive;
import com.apptentive.android.sdk.ApptentiveInternal;
import com.apptentive.android.sdk.lifecycle.ApptentiveActivityLifecycleCallbacks;
import com.apptentive.android.sdk.model.CommerceExtendedData;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;

import org.json.JSONException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ApptentiveKit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.CommerceListener, KitIntegration.AttributeListener {
	private static final String API_KEY = "appKey";
	private ApptentiveActivityLifecycleCallbacks callbacks;

	@Override
	public String getName() {
		return "Apptentive";
	}

	@Override
	protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
		String apiKey = settings.get(API_KEY);
		if (KitUtils.isEmpty(apiKey)) {
			throw new IllegalArgumentException("Apptentive app key is required.");
		}
		if (callbacks == null) {
			callbacks = new ApptentiveActivityLifecycleCallbacks();
		}
		((Application)context.getApplicationContext()).registerActivityLifecycleCallbacks(callbacks);
		ApptentiveInternal.createInstance(context.getApplicationContext(), apiKey);
		return null;
	}

	@Override
	public List<ReportingMessage> setOptOut(boolean optedOut) {
		return null;
	}

	@Override
	public void setUserIdentity(MParticle.IdentityType identityType, String id) {
		if (identityType.equals(MParticle.IdentityType.Email)) {
			Apptentive.setPersonEmail(id);
		} else if (identityType.equals(MParticle.IdentityType.CustomerId)) {
			if (KitUtils.isEmpty(Apptentive.getPersonName())) {
				// Use id as customer name iff no full name is set yet.
				Apptentive.setPersonName(id);
			}
		}
	}

	@Override
	public void setUserAttribute(String attributeKey, String attributeValue) {
		String firstName = "";
		String lastName = "";

		if (attributeKey.equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
			firstName = attributeValue;
		} else if (attributeKey.equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
			lastName = attributeValue;
		} else {
			Apptentive.addCustomPersonData(attributeKey, attributeValue);
		}

		String fullName;
		if (!KitUtils.isEmpty(firstName) && !KitUtils.isEmpty(lastName)) {
			fullName = firstName + " " + lastName;
		} else {
			fullName = firstName + lastName;
		}
		Apptentive.setPersonName(fullName.trim());
	}

	@Override
	public void setUserAttributeList(String key, List<String> list) {

	}

	@Override
	public boolean supportsAttributeLists() {
		return false;
	}

	@Override
	public void setAllUserAttributes(Map<String, String> attributes, Map<String, List<String>> attributeLists) {
		String firstName = "";
		String lastName = "";
		for (Map.Entry<String, String> entry : attributes.entrySet()){
			if (entry.getKey().equalsIgnoreCase(MParticle.UserAttributes.FIRSTNAME)) {
				firstName = entry.getValue();
			} else if (entry.getKey().equalsIgnoreCase(MParticle.UserAttributes.LASTNAME)) {
				lastName = entry.getValue();
			} else {
				Apptentive.addCustomPersonData(entry.getKey(), entry.getValue());
			}
		}
		String fullName;
		if (!KitUtils.isEmpty(firstName) && !KitUtils.isEmpty(lastName)) {
			fullName = firstName + " " + lastName;
		} else {
			fullName = firstName + lastName;
		}
		Apptentive.setPersonName(fullName.trim());
	}

	@Override
	public void removeUserAttribute(String key) {
		Apptentive.removeCustomPersonData(key);
	}


	@Override
	public void removeUserIdentity(MParticle.IdentityType identityType) {

	}

	@Override
	public List<ReportingMessage> logout() {
		return null;
	}

	@Override
	public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
		return null;
	}

	@Override
	public List<ReportingMessage> logError(String message, Map<String, String> errorAttributes) {
		return null;
	}

	@Override
	public List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message) {
		return null;
	}

	@Override
	public List<ReportingMessage> logEvent(MPEvent event) {
		Map<String, String> customData = event.getInfo();
		if (customData != null) {
			Apptentive.engage(getContext(), event.getEventName(), Collections.<String, Object>unmodifiableMap(customData));
		} else {
			Apptentive.engage(getContext(), event.getEventName());
		}
		List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
		messageList.add(ReportingMessage.fromEvent(this, event));
		return messageList;
	}

	@Override
	public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
		return null;
	}

	@Override
	public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
		return null;
	}

	@Override
	public List<ReportingMessage> logEvent(CommerceEvent event) {
		if (!KitUtils.isEmpty(event.getProductAction())) {
			try {
				Map<String, String> eventActionAttributes = new HashMap<String, String>();
				CommerceEventUtils.extractActionAttributes(event, eventActionAttributes);

				CommerceExtendedData apptentiveCommerceData = null;

				TransactionAttributes transactionAttributes = event.getTransactionAttributes();
				if (transactionAttributes != null) {
					apptentiveCommerceData = new CommerceExtendedData();

					String transaction_id = transactionAttributes.getId();
					if (!KitUtils.isEmpty(transaction_id)) {
						apptentiveCommerceData.setId(transaction_id);
					}
					Double transRevenue = transactionAttributes.getRevenue();
					if (transRevenue != null) {
						apptentiveCommerceData.setRevenue(transRevenue);
					}
					Double transShipping = transactionAttributes.getShipping();
					if (transShipping != null) {
						apptentiveCommerceData.setShipping(transShipping);
					}
					Double transTax = transactionAttributes.getTax();
					if (transTax != null) {
						apptentiveCommerceData.setTax(transTax);
					}
					String transAffiliation = transactionAttributes.getAffiliation();
					if (!KitUtils.isEmpty(transAffiliation)) {
						apptentiveCommerceData.setAffiliation(transAffiliation);
					}
					String transCurrency = eventActionAttributes.get(CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE);
					if (KitUtils.isEmpty(transCurrency)) {
						transCurrency = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE;
					}
					apptentiveCommerceData.setCurrency(transCurrency);

					// Add each item
					List<Product> productList = event.getProducts();
					if (productList != null) {
						for (Product product : productList) {
							CommerceExtendedData.Item item = new CommerceExtendedData.Item();
							item.setId(product.getSku());
							item.setName(product.getName());
							item.setCategory(product.getCategory());
							item.setPrice(product.getUnitPrice());
							item.setQuantity(product.getQuantity());
							item.setCurrency(transCurrency);
							apptentiveCommerceData.addItem(item);
						}
					}
				}


				if (apptentiveCommerceData != null) {
					Map<String, String> customData = event.getCustomAttributes();
					Apptentive.engage(getContext(),
							String.format("eCommerce - %s", event.getProductAction()),
							customData == null ? null : Collections.<String, Object>unmodifiableMap(customData),
							apptentiveCommerceData);
					List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
					messages.add(ReportingMessage.fromEvent(this, event));
					return messages;
				}
			}catch (JSONException jse) {

			}

		}
		return null;
	}
}