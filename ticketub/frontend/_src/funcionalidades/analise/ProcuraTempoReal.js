import React, { useState, useEffect, useCallback } from 'react';
import { getTempoReal, getPorHorario, getPorLinha, getPorParagem } from '../../logica_do_sistema/services/analiseService';
import './Analise.css';

function ProcuraTempoReal() {
  const [activeTab, setActiveTab] = useState('horario');
  const [liveData, setLiveData] = useState(null);
  const [tabData, setTabData] = useState(null);
  const [loadingLive, setLoadingLive] = useState(true);
  const [loadingTab, setLoadingTab] = useState(true);

  const fetchLiveData = useCallback(async (isSilent = false) => {
    if (!isSilent) setLoadingLive(true);
    try {
      const data = await getTempoReal();
      setLiveData(data);
    } catch (err) {
      setLiveData(null);
    } finally {
      if (!isSilent) setLoadingLive(false);
    }
  }, []);

  const fetchTabData = useCallback(async () => {
    setLoadingTab(true);
    try {
      let data;
      if (activeTab === 'horario') {
        data = await getPorHorario();
      } else if (activeTab === 'linha') {
        data = await getPorLinha();
      } else if (activeTab === 'paragem') {
        data = await getPorParagem();
      }
      setTabData(data);
    } catch (err) {
      setTabData(null);
    } finally {
      setLoadingTab(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchLiveData();
    
    const intervalId = setInterval(() => {
      fetchLiveData(true);
    }, 3000);

    return () => clearInterval(intervalId);
  }, [fetchLiveData]);

  useEffect(() => {
    fetchTabData();
  }, [fetchTabData]);

  const renderTable = (dataArray) => {
    if (!dataArray || !Array.isArray(dataArray) || dataArray.length === 0) {
      return <div className="analise-empty-state">Sem dados disponíveis</div>;
    }

    const headers = Object.keys(dataArray[0]);

    return (
      <div className="analise-table-container">
        <table className="analise-table">
          <thead>
            <tr>
              {headers.map(h => <th key={h}>{h.charAt(0).toUpperCase() + h.slice(1)}</th>)}
            </tr>
          </thead>
          <tbody>
            {dataArray.map((row, idx) => (
              <tr key={idx}>
                {headers.map(h => <td key={`${idx}-${h}`}>{String(row[h])}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="analise-container">
      {/* Live Card */}
      <div className="analise-card">
        <div className="analise-card-header">
          <h2>
            Métricas em Tempo Real
            <span className="analise-live-badge">
              <span className="analise-live-dot"></span>
              Live
            </span>
          </h2>
        </div>
        <div className="analise-card-body">
          {loadingLive && !liveData ? (
             <div className="analise-empty-state">A carregar...</div>
          ) : !liveData ? (
             <div className="analise-empty-state">Sem dados disponíveis</div>
          ) : (
            <div className="historico-metrics-grid">
              {Object.entries(liveData).map(([key, value]) => (
                 <div key={key} className="historico-metric-box">
                    <span className="historico-metric-label">{key}</span>
                    <span className="historico-metric-value">{String(value)}</span>
                 </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Tabs Card */}
      <div className="analise-card">
        <div className="analise-tabs">
          <button 
            className={`analise-tab ${activeTab === 'horario' ? 'active' : ''}`}
            onClick={() => setActiveTab('horario')}
          >
            Por Horário
          </button>
          <button 
            className={`analise-tab ${activeTab === 'linha' ? 'active' : ''}`}
            onClick={() => setActiveTab('linha')}
          >
            Por Linha
          </button>
          <button 
            className={`analise-tab ${activeTab === 'paragem' ? 'active' : ''}`}
            onClick={() => setActiveTab('paragem')}
          >
            Por Paragem
          </button>
        </div>
        
        <div className="analise-card-body">
          {loadingTab ? (
            <div className="analise-empty-state">A carregar...</div>
          ) : (
            renderTable(tabData)
          )}
        </div>
      </div>
    </div>
  );
}

export default ProcuraTempoReal;
