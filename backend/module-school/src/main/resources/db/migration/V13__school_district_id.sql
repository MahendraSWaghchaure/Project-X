-- Optional structured link into module-geo's reference data, additive alongside the existing
-- free-text city/state/pincode columns - see School.java's districtId javadoc.
ALTER TABLE schools ADD COLUMN district_id BIGINT;
CREATE INDEX idx_schools_district_id ON schools (district_id);
