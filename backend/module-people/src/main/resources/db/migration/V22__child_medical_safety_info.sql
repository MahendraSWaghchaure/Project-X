-- Safety-relevant info worth having on hand for a school transport product: blood type for any
-- medical situation during a trip, an emergency contact distinct from (and reachable when) the
-- guardian accounts aren't, and a free-text allergy/medical-note field a driver or first responder
-- can act on without digging through app screens.
ALTER TABLE children ADD COLUMN blood_type VARCHAR(8);
ALTER TABLE children ADD COLUMN emergency_contact_name VARCHAR(255);
ALTER TABLE children ADD COLUMN emergency_contact_phone VARCHAR(32);
ALTER TABLE children ADD COLUMN allergies VARCHAR(500);
