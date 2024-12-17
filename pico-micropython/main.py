from itk_pico.stepper_uln2003 import StepperMotor
from itk_pico.temperature import TemperatureSensor
from itk_pico.time import Time
from itk_pico.wifi import WiFi
from itk_pico.udp import Udp
from itk_pico.logger import Logger
from itk_pico.ip_utils import ip_2_broadcast
import json
import time

UDP_PORT = 65432
# 1 hour in seconds
COOL_DOWN = 60 * 60

DISCOVERY_MESSAGE = "Who is there?"
OPEN_MESSAGE = "Knock-Knock, Open Up!"
CLOSE_MESSAGE = "Shut the Door!"

last_udp_command_time = 0

wifi = WiFi()
date_time = Time(-7)
temperature_sensor = TemperatureSensor(15)
motor = StepperMotor()
udp = Udp(UDP_PORT)

wifi.connect("itkacher", "itkacher")
date_time.sync()
is_opened = True
motor.rotate(direction=-1)
udp.start_listening(is_blocking=False)

while True:
    wifi.try_reconnect_if_lost()
    message, address, _ = udp.read()
    if message is not None and address is not None:
        if DISCOVERY_MESSAGE in message:
            temperature = temperature_sensor.get_temperature()
            message_dict = {"name": "Bedroom" ,"temperature": temperature, "is_opened": is_opened, "sender_ip": address}
            message = json.dumps(message_dict)
            udp.send(message, ip_2_broadcast(address), UDP_PORT)
        elif OPEN_MESSAGE in message:
            is_opened = True
            motor.rotate(direction=-1)
            last_udp_command_time = date_time.current_timestamp_seconds()
        elif CLOSE_MESSAGE in message:
            is_opened = False
            motor.rotate(direction=1)
            last_udp_command_time = date_time.current_timestamp_seconds()

    if date_time.current_timestamp_seconds() - last_udp_command_time < COOL_DOWN: 
        continue

    hour = date_time.current_hour()
    if hour == None:
        pass
    # Close registry during a day
    elif hour >= 7 and hour < 20 and is_opened: 
        Logger.print("Closing during a day")
        motor.rotate(direction=-1)
        is_opened = False
    # Open registry at night
    elif hour < 7 and not is_opened or  hour > 20 and not is_opened:
        Logger.print("Opening at night")
        is_opened = True
        motor.rotate(direction=1)
