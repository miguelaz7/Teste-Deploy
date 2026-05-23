import React, { useState, useEffect, useCallback } from 'react';
import { getTempoReal, getPorHorario, getPorLinha, getPorParagem } from '../../logica_do_sistema/services/analiseService';
import './Analise.css';

const LABELS = {
  timeGap:                    'Hora de Pico',
  peakAfluenciaPercentage:    'Afluência no Pico (%)',
  invalidCount:               'Inválidas',
  invalidPercentage:          'Taxa de Inválidas (%)',
  total:                      'Total de Validações',
  stopId:                     'Paragem',
  perspectiva:                'Perspectiva',
  chave:                      'Chave',
  totalValidacoes:            'Total Validações',
  totalInvalidas:             'Inválidas',
  perfilEstudante:            'Estudante',
  perfilSenior:               'Sénior',
  perfilNormal:               'Normal',
  actualizadoEm:              'Actualizado em',
};

const label = (key) => LABELS[key] || key;

const fmt = (key, value) => {
  if (value === null || value === undefined) return '—';
  if (key === 'stopId' && !value) return 'Todas';
  if (typeof value === 'number' && key.toLowerCase().includes('percent')) return `${value}%`;
  if (typeof value === 'string' && value.includes('T') && value.includes(':')) {
    try { return new Date(value).toLocaleString('pt-PT'); } catch { return value; }
  }
  return String(value);
};

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
    } catch { setLiveData(null); }
    finally { if (!isSilent) setLoadingLive(false); }
  }, []);

  const fetchTabData = useCallback(async () => {
    setLoadingTab(true);
    try {
      let data;
      if (activeTab === 'horario') data = await getPorHorario();
      else if (activeTab === 'linha') data = await getPorLinha();
      else if (activeTab === 'paragem') data = await getPorParagem();
      setTabData(data);
    } catch { setTabData(null); }
    finally { setLoadingTab(false); }
  }, [activeTab]);

  useEffect(() => {
    fetchLiveData();
    const id = setInterval(() => fetchLiveData(true), 5000);
    return () => clearInterval(id);
  }, [fetchLiveData]);

  useEffect(() => { fetchTabData(); }, [fetchTabData]);

  const renderLive = () => {
    if (!liveData) return <div className="analise-empty-state">Sem dados disponíveis</div>;
    const campos = ['total', 'timeGap', 'peakAfluenciaPercentage', 'invalidCount', 'invalidPercentage'];
    return (
      <div className="historico-metrics-grid">
        {campos.filter(k => k in liveData).map(k => (
          <div key={k} className="historico-metric-box">
            <span className="historico-metric-label">{label(k)}</span>
            <span className="historico-metric-value">{fmt(k, liveData[k])}</span>
          </div>
        ))}
      </div>
    );
  };

  const renderTable = (dataArray) => {
    if (!dataArray || !Array.isArray(dataArray) || dataArray.length === 0)
      return <div className="analise-empty-state">Sem dados disponíveis</div>;

    const cols = ['chave', 'totalValidacoes', 'totalInvalidas', 'perfilEstudante', 'perfilSenior', 'perfilNormal'];
    const headers = cols.filter(c => c in dataArray[0]);

    return (
      <div className="analise-table-container">
        <table className="analise-table">
          <thead>
            <tr>{headers.map(h => <th key={h}>{label(h)}</th>)}</tr>
          </thead>
          <tbody>
            {dataArray.map((row, idx) => (
              <tr key={idx}>
                {headers.map(h => <td key={`${idx}-${h}`}>{fmt(h, row[h])}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="analise-container">
      <div className="analise-card">
        <div className="analise-card-header">
          <h2>
            Métricas em Tempo Real
            <span className="analise-live-badge">
              <span className="analise-live-dot"></span>Live
            </span>
          </h2>
        </div>
        <div className="analise-card-body">
          {loadingLive && !liveData
            ? <div className="analise-empty-state">A carregar...</div>
            : renderLive()}
        </div>
      </div>

      <div className="analise-card">
        <div className="analise-tabs">
          {['horario','linha','paragem'].map(t => (
            <button key={t}
              className={`analise-tab ${activeTab === t ? 'active' : ''}`}
              onClick={() => setActiveTab(t)}>
              {t === 'horario' ? 'Por Horário' : t === 'linha' ? 'Por Linha' : 'Por Paragem'}
            </button>
          ))}
        </div>
        <div className="analise-card-body">
          {loadingTab
            ? <div className="analise-empty-state">A carregar...</div>
            : renderTable(tabData)}
        </div>
      </div>
    </div>
  );
}

export default ProcuraTempoReal;