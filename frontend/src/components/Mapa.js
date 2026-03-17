import React from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './Mapa.css';

// Fix for default marker icon in Leaflet + React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const ParagemUniversidade = [41.557583779603355, -8.397569317003903];

function Mapa() {
  return (
    <div className="map-wrapper">
      <div className="map-header">
        <h2>Mapa de Rede</h2>
        <p>Explore as paragens e rotas dos TUB em Braga.</p>
      </div>
      <div className="map-container-inner">
        <MapContainer
          center={ParagemUniversidade}
          zoom={13}
          scrollWheelZoom={true}
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker position={ParagemUniversidade}>
            <Popup>
              Paragem Universidade <br /> do Minho.
            </Popup>
          </Marker>
        </MapContainer>
      </div>
    </div>
  );
}

export default Mapa;
