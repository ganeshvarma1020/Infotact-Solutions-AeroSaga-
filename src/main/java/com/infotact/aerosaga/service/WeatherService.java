package com.infotact.aerosaga.service;

import com.infotact.aerosaga.model.WeatherReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Service simulating real-world meteorological data acquisition (e.g. NOAA/OpenWeather integration).
 */
@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private final Random random = new Random();

    public WeatherReport checkWeather(double latitude, double longitude, boolean simulateSevereWeather) {
        if (simulateSevereWeather) {
            logger.warn("Simulated severe weather conditions triggered for coords ({}, {})", latitude, longitude);
            return new WeatherReport(
                    false,
                    34.5, // 34.5 knots (> 25 kt limit)
                    42.0,
                    1.2,  // 1.2 km poor visibility
                    28.5, // 28.5 mm heavy rain
                    true, // thunderstorm alert active
                    "SEVERE WEATHER ALERT: Level 4 squall line, high crosswinds and thunderstorm activity detected."
            );
        }

        // Normal nominal flying weather
        double wind = 6.0 + (random.nextDouble() * 6.0); // 6 - 12 knots
        double gust = wind + (random.nextDouble() * 4.0);
        double visibility = 9.0 + (random.nextDouble() * 2.0); // 9 - 11 km
        double rain = random.nextDouble() < 0.2 ? 0.4 : 0.0;

        return new WeatherReport(
                true,
                Math.round(wind * 10.0) / 10.0,
                Math.round(gust * 10.0) / 10.0,
                Math.round(visibility * 10.0) / 10.0,
                Math.round(rain * 10.0) / 10.0,
                false,
                "NOMINAL: VFR (Visual Flight Rules) clear skies, moderate breeze within operational envelope."
        );
    }
}
