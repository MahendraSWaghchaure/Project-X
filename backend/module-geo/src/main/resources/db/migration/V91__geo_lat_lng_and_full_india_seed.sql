-- module-geo owns Flyway version range V90-V99 (continues V90).
-- B2: adds representative lat/lng to states+districts (for B3's map fly-to) and expands the seed
-- from the original 6-state/~20-district sample to all 28 states + 8 union territories, with a
-- broad (not exhaustive - see PLAN-phase3's Contract deviations) set of districts per state.
-- Coordinates are approximate (state centroid / district HQ town), bundled at build time - no live
-- geocoding API call, consistent with the single-VM/no-new-infra/no-internet-dependency constraint.

ALTER TABLE geo_states ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE geo_states ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE geo_districts ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE geo_districts ADD COLUMN longitude DOUBLE PRECISION;

-- Backfill the 6 states seeded by V90.
UPDATE geo_states SET latitude = 19.7515, longitude = 75.7139 WHERE name = 'Maharashtra';
UPDATE geo_states SET latitude = 15.3173, longitude = 75.7139 WHERE name = 'Karnataka';
UPDATE geo_states SET latitude = 28.7041, longitude = 77.1025 WHERE name = 'Delhi';
UPDATE geo_states SET latitude = 11.1271, longitude = 78.6569 WHERE name = 'Tamil Nadu';
UPDATE geo_states SET latitude = 22.2587, longitude = 71.1924 WHERE name = 'Gujarat';
UPDATE geo_states SET latitude = 26.8467, longitude = 80.9462 WHERE name = 'Uttar Pradesh';

-- Backfill the ~20 districts seeded by V90.
UPDATE geo_districts SET latitude = 19.9975, longitude = 73.7898 WHERE name = 'Nashik';
UPDATE geo_districts SET latitude = 18.5204, longitude = 73.8567 WHERE name = 'Pune';
UPDATE geo_districts SET latitude = 18.9388, longitude = 72.8354 WHERE name = 'Mumbai City';
UPDATE geo_districts SET latitude = 19.0760, longitude = 72.8777 WHERE name = 'Mumbai Suburban';
UPDATE geo_districts SET latitude = 21.1458, longitude = 79.0882 WHERE name = 'Nagpur';
UPDATE geo_districts SET latitude = 12.9716, longitude = 77.5946 WHERE name = 'Bengaluru Urban';
UPDATE geo_districts SET latitude = 12.2958, longitude = 76.6394 WHERE name = 'Mysuru';
UPDATE geo_districts SET latitude = 28.6139, longitude = 77.2090 WHERE name = 'New Delhi';
UPDATE geo_districts SET latitude = 28.5355, longitude = 77.2500 WHERE name = 'South Delhi';
UPDATE geo_districts SET latitude = 13.0827, longitude = 80.2707 WHERE name = 'Chennai';
UPDATE geo_districts SET latitude = 11.0168, longitude = 76.9558 WHERE name = 'Coimbatore';
UPDATE geo_districts SET latitude = 9.9252, longitude = 78.1198 WHERE name = 'Madurai';
UPDATE geo_districts SET latitude = 23.0225, longitude = 72.5714 WHERE name = 'Ahmedabad';
UPDATE geo_districts SET latitude = 21.1702, longitude = 72.8311 WHERE name = 'Surat';
UPDATE geo_districts SET latitude = 22.3072, longitude = 73.1812 WHERE name = 'Vadodara';
UPDATE geo_districts SET latitude = 26.8467, longitude = 80.9462 WHERE name = 'Lucknow';
UPDATE geo_districts SET latitude = 26.4499, longitude = 80.3319 WHERE name = 'Kanpur Nagar';
UPDATE geo_districts SET latitude = 25.3176, longitude = 82.9739 WHERE name = 'Varanasi';

-- New states (22 more states + 8 union territories = 30 new rows; Delhi already exists from V90).
INSERT INTO geo_states (country_id, name, code, latitude, longitude)
SELECT id, s.name, s.code, s.lat, s.lng FROM geo_countries, (VALUES
    ('Andhra Pradesh', 'AP', 15.9129, 79.7400),
    ('Arunachal Pradesh', 'AR', 28.2180, 94.7278),
    ('Assam', 'AS', 26.2006, 92.9376),
    ('Bihar', 'BR', 25.0961, 85.3131),
    ('Chhattisgarh', 'CG', 21.2787, 81.8661),
    ('Goa', 'GA', 15.2993, 74.1240),
    ('Haryana', 'HR', 29.0588, 76.0856),
    ('Himachal Pradesh', 'HP', 31.1048, 77.1734),
    ('Jharkhand', 'JH', 23.6102, 85.2799),
    ('Kerala', 'KL', 10.8505, 76.2711),
    ('Madhya Pradesh', 'MP', 22.9734, 78.6569),
    ('Manipur', 'MN', 24.6637, 93.9063),
    ('Meghalaya', 'ML', 25.4670, 91.3662),
    ('Mizoram', 'MZ', 23.1645, 92.9376),
    ('Nagaland', 'NL', 26.1584, 94.5624),
    ('Odisha', 'OD', 20.9517, 85.0985),
    ('Punjab', 'PB', 31.1471, 75.3412),
    ('Rajasthan', 'RJ', 27.0238, 74.2179),
    ('Sikkim', 'SK', 27.5330, 88.5122),
    ('Telangana', 'TG', 18.1124, 79.0193),
    ('Tripura', 'TR', 23.9408, 91.9882),
    ('Uttarakhand', 'UK', 30.0668, 79.0193),
    ('West Bengal', 'WB', 22.9868, 87.8550),
    ('Andaman and Nicobar Islands', 'AN', 11.7401, 92.6586),
    ('Chandigarh', 'CH', 30.7333, 76.7794),
    ('Dadra and Nagar Haveli and Daman and Diu', 'DN', 20.1809, 73.0169),
    ('Jammu and Kashmir', 'JK', 33.7782, 76.5762),
    ('Ladakh', 'LA', 34.1526, 77.5771),
    ('Lakshadweep', 'LD', 10.5667, 72.6417),
    ('Puducherry', 'PY', 11.9416, 79.8083)
) AS s(name, code, lat, lng)
WHERE geo_countries.code = 'IN';

-- New districts, grouped by state. Coverage is broad but not exhaustive for the largest states
-- (see PLAN-phase3-polish-geo-timetable.md's Contract deviations) - more can be added the same way.
INSERT INTO geo_districts (state_id, region_id, name, latitude, longitude)
SELECT s.id, NULL, d.name, d.lat, d.lng
FROM geo_states s
CROSS JOIN LATERAL (VALUES
    ('Andhra Pradesh', 'Srikakulam', 18.2949, 83.8938),
    ('Andhra Pradesh', 'Vizianagaram', 18.1067, 83.4116),
    ('Andhra Pradesh', 'Visakhapatnam', 17.6868, 83.2185),
    ('Andhra Pradesh', 'East Godavari', 16.9891, 82.2475),
    ('Andhra Pradesh', 'West Godavari', 16.9174, 81.3399),
    ('Andhra Pradesh', 'Krishna', 16.5062, 80.6480),
    ('Andhra Pradesh', 'Guntur', 16.3067, 80.4365),
    ('Andhra Pradesh', 'Prakasam', 15.5057, 80.0499),
    ('Andhra Pradesh', 'Nellore', 14.4426, 79.9865),
    ('Andhra Pradesh', 'Chittoor', 13.2172, 79.1003),
    ('Andhra Pradesh', 'Kadapa', 14.4673, 78.8242),
    ('Andhra Pradesh', 'Anantapur', 14.6819, 77.6006),
    ('Andhra Pradesh', 'Kurnool', 15.8281, 78.0373),

    ('Arunachal Pradesh', 'Papum Pare', 27.1000, 93.6200),
    ('Arunachal Pradesh', 'West Kameng', 27.2167, 92.4000),
    ('Arunachal Pradesh', 'East Siang', 28.0667, 95.3333),
    ('Arunachal Pradesh', 'Lower Subansiri', 27.5667, 93.8167),
    ('Arunachal Pradesh', 'Tawang', 27.5861, 91.8594),
    ('Arunachal Pradesh', 'Changlang', 27.1333, 95.6667),

    ('Assam', 'Kamrup Metropolitan', 26.1445, 91.7362),
    ('Assam', 'Dibrugarh', 27.4728, 94.9120),
    ('Assam', 'Jorhat', 26.7509, 94.2037),
    ('Assam', 'Sivasagar', 26.9850, 94.6380),
    ('Assam', 'Nagaon', 26.3500, 92.6833),
    ('Assam', 'Barpeta', 26.4833, 90.9667),
    ('Assam', 'Cachar', 24.8333, 92.7789),
    ('Assam', 'Tinsukia', 27.4922, 95.3558),
    ('Assam', 'Golaghat', 26.5167, 93.9667),
    ('Assam', 'Dhubri', 26.0200, 89.9850),

    ('Bihar', 'Patna', 25.5941, 85.1376),
    ('Bihar', 'Gaya', 24.7955, 84.9994),
    ('Bihar', 'Bhagalpur', 25.2425, 86.9842),
    ('Bihar', 'Muzaffarpur', 26.1225, 85.3906),
    ('Bihar', 'Darbhanga', 26.1542, 85.8918),
    ('Bihar', 'Purnia', 25.7771, 87.4753),
    ('Bihar', 'Begusarai', 25.4182, 86.1272),
    ('Bihar', 'Nalanda', 25.1341, 85.4435),
    ('Bihar', 'Saran', 25.9199, 84.7515),
    ('Bihar', 'Bhojpur', 25.5586, 84.6639),

    ('Chhattisgarh', 'Raipur', 21.2514, 81.6296),
    ('Chhattisgarh', 'Bilaspur', 22.0797, 82.1409),
    ('Chhattisgarh', 'Durg', 21.1904, 81.2849),
    ('Chhattisgarh', 'Korba', 22.3595, 82.7501),
    ('Chhattisgarh', 'Raigarh', 21.8974, 83.3950),
    ('Chhattisgarh', 'Bastar', 19.0748, 82.0280),
    ('Chhattisgarh', 'Rajnandgaon', 21.0974, 81.0289),

    ('Goa', 'North Goa', 15.5667, 73.8333),
    ('Goa', 'South Goa', 15.1667, 74.0167),

    ('Haryana', 'Gurugram', 28.4595, 77.0266),
    ('Haryana', 'Faridabad', 28.4089, 77.3178),
    ('Haryana', 'Panipat', 29.3909, 76.9635),
    ('Haryana', 'Ambala', 30.3782, 76.7767),
    ('Haryana', 'Hisar', 29.1492, 75.7217),
    ('Haryana', 'Karnal', 29.6857, 76.9905),
    ('Haryana', 'Rohtak', 28.8955, 76.6066),
    ('Haryana', 'Yamunanagar', 30.1290, 77.2674),
    ('Haryana', 'Sonipat', 28.9931, 77.0151),
    ('Haryana', 'Panchkula', 30.6942, 76.8606),

    ('Himachal Pradesh', 'Shimla', 31.1048, 77.1734),
    ('Himachal Pradesh', 'Kangra', 32.0998, 76.2691),
    ('Himachal Pradesh', 'Mandi', 31.7084, 76.9319),
    ('Himachal Pradesh', 'Solan', 30.9045, 77.0967),
    ('Himachal Pradesh', 'Kullu', 31.9578, 77.1095),
    ('Himachal Pradesh', 'Una', 31.4685, 76.2708),
    ('Himachal Pradesh', 'Hamirpur', 31.6861, 76.5217),
    ('Himachal Pradesh', 'Bilaspur', 31.3410, 76.7566),
    ('Himachal Pradesh', 'Chamba', 32.5534, 76.1258),
    ('Himachal Pradesh', 'Sirmaur', 30.5666, 77.2914),

    ('Jharkhand', 'Ranchi', 23.3441, 85.3096),
    ('Jharkhand', 'Dhanbad', 23.7957, 86.4304),
    ('Jharkhand', 'East Singhbhum', 22.8046, 86.2029),
    ('Jharkhand', 'Bokaro', 23.6693, 86.1511),
    ('Jharkhand', 'Hazaribagh', 23.9925, 85.3637),
    ('Jharkhand', 'Deoghar', 24.4826, 86.6950),
    ('Jharkhand', 'Giridih', 24.1913, 86.3000),
    ('Jharkhand', 'Palamu', 24.0432, 84.0677),
    ('Jharkhand', 'Dumka', 24.2667, 87.2500),
    ('Jharkhand', 'Garhwa', 24.1555, 83.8073),

    ('Kerala', 'Thiruvananthapuram', 8.5241, 76.9366),
    ('Kerala', 'Kollam', 8.8932, 76.6141),
    ('Kerala', 'Pathanamthitta', 9.2648, 76.7870),
    ('Kerala', 'Alappuzha', 9.4981, 76.3388),
    ('Kerala', 'Kottayam', 9.5916, 76.5222),
    ('Kerala', 'Idukki', 9.8497, 76.9700),
    ('Kerala', 'Ernakulam', 9.9816, 76.2999),
    ('Kerala', 'Thrissur', 10.5276, 76.2144),
    ('Kerala', 'Palakkad', 10.7867, 76.6548),
    ('Kerala', 'Malappuram', 11.0510, 76.0711),
    ('Kerala', 'Kozhikode', 11.2588, 75.7804),
    ('Kerala', 'Wayanad', 11.6854, 76.1320),
    ('Kerala', 'Kannur', 11.8745, 75.3704),
    ('Kerala', 'Kasaragod', 12.4996, 74.9869),

    ('Madhya Pradesh', 'Bhopal', 23.2599, 77.4126),
    ('Madhya Pradesh', 'Indore', 22.7196, 75.8577),
    ('Madhya Pradesh', 'Jabalpur', 23.1815, 79.9864),
    ('Madhya Pradesh', 'Gwalior', 26.2183, 78.1828),
    ('Madhya Pradesh', 'Ujjain', 23.1793, 75.7849),
    ('Madhya Pradesh', 'Sagar', 23.8388, 78.7378),
    ('Madhya Pradesh', 'Satna', 24.6005, 80.8322),
    ('Madhya Pradesh', 'Rewa', 24.5362, 81.3037),
    ('Madhya Pradesh', 'Ratlam', 23.3315, 75.0367),
    ('Madhya Pradesh', 'Dewas', 22.9676, 76.0534),

    ('Manipur', 'Imphal West', 24.8170, 93.9368),
    ('Manipur', 'Imphal East', 24.8000, 94.0000),
    ('Manipur', 'Churachandpur', 24.3333, 93.6833),
    ('Manipur', 'Thoubal', 24.6333, 93.9833),
    ('Manipur', 'Bishnupur', 24.6333, 93.7667),

    ('Meghalaya', 'East Khasi Hills', 25.5788, 91.8933),
    ('Meghalaya', 'West Garo Hills', 25.5138, 90.2172),
    ('Meghalaya', 'Jaintia Hills', 25.4500, 92.3667),
    ('Meghalaya', 'Ri Bhoi', 25.9000, 91.8833),

    ('Mizoram', 'Aizawl', 23.7271, 92.7176),
    ('Mizoram', 'Lunglei', 22.8879, 92.7392),
    ('Mizoram', 'Champhai', 23.4512, 93.3273),

    ('Nagaland', 'Kohima', 25.6751, 94.1086),
    ('Nagaland', 'Dimapur', 25.9091, 93.7266),
    ('Nagaland', 'Mokokchung', 26.3236, 94.5190),
    ('Nagaland', 'Wokha', 26.0969, 94.2645),

    ('Odisha', 'Khordha', 20.1821, 85.6188),
    ('Odisha', 'Cuttack', 20.4625, 85.8830),
    ('Odisha', 'Puri', 19.8135, 85.8312),
    ('Odisha', 'Ganjam', 19.3850, 84.8000),
    ('Odisha', 'Sambalpur', 21.4669, 83.9756),
    ('Odisha', 'Balasore', 21.4942, 86.9335),
    ('Odisha', 'Mayurbhanj', 21.9333, 86.7333),
    ('Odisha', 'Koraput', 18.8120, 82.7100),
    ('Odisha', 'Kalahandi', 19.9137, 83.1657),
    ('Odisha', 'Sundargarh', 22.2604, 84.8536),

    ('Punjab', 'Ludhiana', 30.9010, 75.8573),
    ('Punjab', 'Amritsar', 31.6340, 74.8723),
    ('Punjab', 'Jalandhar', 31.3260, 75.5762),
    ('Punjab', 'Patiala', 30.3398, 76.3869),
    ('Punjab', 'Bathinda', 30.2110, 74.9455),
    ('Punjab', 'Mohali', 30.7046, 76.7179),
    ('Punjab', 'Hoshiarpur', 31.5322, 75.9115),
    ('Punjab', 'Gurdaspur', 32.0409, 75.4051),
    ('Punjab', 'Ferozepur', 30.9331, 74.6225),
    ('Punjab', 'Moga', 30.8158, 75.1711),

    ('Rajasthan', 'Jaipur', 26.9124, 75.7873),
    ('Rajasthan', 'Jodhpur', 26.2389, 73.0243),
    ('Rajasthan', 'Udaipur', 24.5854, 73.7125),
    ('Rajasthan', 'Kota', 25.2138, 75.8648),
    ('Rajasthan', 'Ajmer', 26.4499, 74.6399),
    ('Rajasthan', 'Bikaner', 28.0229, 73.3119),
    ('Rajasthan', 'Alwar', 27.5530, 76.6346),
    ('Rajasthan', 'Bharatpur', 27.2152, 77.4910),
    ('Rajasthan', 'Sikar', 27.6094, 75.1399),
    ('Rajasthan', 'Bhilwara', 25.3407, 74.6313),

    ('Sikkim', 'East Sikkim', 27.3389, 88.6065),
    ('Sikkim', 'West Sikkim', 27.2833, 88.2167),
    ('Sikkim', 'North Sikkim', 27.7167, 88.5333),
    ('Sikkim', 'South Sikkim', 27.1667, 88.3500),

    ('Telangana', 'Hyderabad', 17.3850, 78.4867),
    ('Telangana', 'Rangareddy', 17.3000, 78.4000),
    ('Telangana', 'Warangal', 17.9689, 79.5941),
    ('Telangana', 'Nizamabad', 18.6725, 78.0941),
    ('Telangana', 'Karimnagar', 18.4386, 79.1288),
    ('Telangana', 'Khammam', 17.2473, 80.1514),
    ('Telangana', 'Mahbubnagar', 16.7488, 77.9855),
    ('Telangana', 'Nalgonda', 17.0575, 79.2690),
    ('Telangana', 'Adilabad', 19.6667, 78.5333),
    ('Telangana', 'Medak', 18.0460, 78.2630),

    ('Tripura', 'West Tripura', 23.8315, 91.2868),
    ('Tripura', 'South Tripura', 23.1667, 91.4333),
    ('Tripura', 'North Tripura', 24.0500, 92.0000),
    ('Tripura', 'Dhalai', 23.8667, 91.8667),

    ('Uttar Pradesh', 'Agra', 27.1767, 78.0081),
    ('Uttar Pradesh', 'Prayagraj', 25.4358, 81.8463),
    ('Uttar Pradesh', 'Meerut', 28.9845, 77.7064),
    ('Uttar Pradesh', 'Ghaziabad', 28.6692, 77.4538),
    ('Uttar Pradesh', 'Gorakhpur', 26.7606, 83.3732),
    ('Uttar Pradesh', 'Bareilly', 28.3670, 79.4304),
    ('Uttar Pradesh', 'Aligarh', 27.8974, 78.0880),
    ('Uttar Pradesh', 'Moradabad', 28.8386, 78.7733),
    ('Uttar Pradesh', 'Jhansi', 25.4484, 78.5685),
    ('Uttar Pradesh', 'Gautam Buddha Nagar', 28.5355, 77.3910),

    ('Uttarakhand', 'Dehradun', 30.3165, 78.0322),
    ('Uttarakhand', 'Haridwar', 29.9457, 78.1642),
    ('Uttarakhand', 'Nainital', 29.3803, 79.4636),
    ('Uttarakhand', 'Udham Singh Nagar', 28.9700, 79.4000),
    ('Uttarakhand', 'Almora', 29.5892, 79.6467),
    ('Uttarakhand', 'Pauri Garhwal', 30.1467, 78.7773),
    ('Uttarakhand', 'Chamoli', 30.4000, 79.3200),
    ('Uttarakhand', 'Tehri Garhwal', 30.3800, 78.4800),

    ('West Bengal', 'Kolkata', 22.5726, 88.3639),
    ('West Bengal', 'Howrah', 22.5958, 88.2636),
    ('West Bengal', 'North 24 Parganas', 22.8000, 88.4000),
    ('West Bengal', 'South 24 Parganas', 22.1000, 88.4000),
    ('West Bengal', 'Darjeeling', 27.0410, 88.2663),
    ('West Bengal', 'Jalpaiguri', 26.5447, 88.7198),
    ('West Bengal', 'Murshidabad', 24.1833, 88.2667),
    ('West Bengal', 'Nadia', 23.4710, 88.5565),
    ('West Bengal', 'Purba Bardhaman', 23.2324, 87.8615),
    ('West Bengal', 'Hooghly', 22.9100, 88.3900),
    ('West Bengal', 'Malda', 25.0108, 88.1411),

    ('Andaman and Nicobar Islands', 'South Andaman', 11.6234, 92.7265),
    ('Chandigarh', 'Chandigarh', 30.7333, 76.7794),
    ('Dadra and Nagar Haveli and Daman and Diu', 'Daman', 20.3974, 72.8328),
    ('Dadra and Nagar Haveli and Daman and Diu', 'Dadra and Nagar Haveli', 20.2666, 73.0167),
    ('Jammu and Kashmir', 'Srinagar', 34.0837, 74.7973),
    ('Jammu and Kashmir', 'Jammu', 32.7266, 74.8570),
    ('Jammu and Kashmir', 'Anantnag', 33.7311, 75.1480),
    ('Jammu and Kashmir', 'Baramulla', 34.2096, 74.3434),
    ('Ladakh', 'Leh', 34.1642, 77.5848),
    ('Ladakh', 'Kargil', 34.5539, 76.1313),
    ('Lakshadweep', 'Lakshadweep', 10.5667, 72.6417),
    ('Puducherry', 'Puducherry', 11.9416, 79.8083),
    ('Puducherry', 'Karaikal', 10.9254, 79.8380)
) AS d(state, name, lat, lng)
WHERE s.name = d.state;

-- From here on every row (old + new) has coordinates - make that the enforced shape going forward.
ALTER TABLE geo_states ALTER COLUMN latitude SET NOT NULL;
ALTER TABLE geo_states ALTER COLUMN longitude SET NOT NULL;
ALTER TABLE geo_districts ALTER COLUMN latitude SET NOT NULL;
ALTER TABLE geo_districts ALTER COLUMN longitude SET NOT NULL;
