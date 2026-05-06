import React, { useState, useEffect, useCallback } from 'react';
import { getDadosAbertos, downloadExportacao } from '../../logica_do_sistema/services/odService';
import './Od.css';

function ExportacaoDados() {
  const [datasets, setDatasets] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchDatasets = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getDadosAbertos();
      // Assumimos que o backend nos envia uma lista de objectos representando os datasets
      setDatasets(Array.isArray(data) ? data : []);
    } catch (err) {
      setDatasets(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDatasets();
  }, [fetchDatasets]);

  const handleDownload = (datasetName) => {
    downloadExportacao(`${datasetName.toLowerCase().replace(/\s+/g, '_')}_export.csv`);
  };

  return (
    <div className="od-card" style={{ backgroundColor: 'transparent', border: 'none', boxShadow: 'none', padding: 0 }}>
      <div className="od-card-header" style={{ marginBottom: '1rem', borderBottom: 'none' }}>
        <h2>Exportação de Dados Abertos</h2>
      </div>
      
      <div className="od-card-body">
        {loading ? (
          <div className="od-empty-state">A carregar datasets disponíveis...</div>
        ) : !datasets ? (
          <div className="od-empty-state">Sem dados disponíveis</div>
        ) : datasets.length === 0 ? (
          <div className="od-empty-state">Não existem datasets abertos no momento.</div>
        ) : (
          <div className="dataset-list">
            {datasets.map((dataset, idx) => {
              // Extrair campos de forma flexível consoante o que a API possa retornar
              const title = dataset.nome || dataset.titulo || dataset.dataset || `Dataset #${idx + 1}`;
              const description = dataset.descricao || dataset.detalhes || 'Dados exportáveis da plataforma TickeTUB para análise externa.';
              const lastUpdated = dataset.ultimaAtualizacao || dataset.updatedAt || new Date().toISOString().split('T')[0];
              
              return (
                <div key={idx} className="dataset-card">
                  <div className="dataset-info">
                    <h3>{title}</h3>
                    <p>{description}</p>
                    <p style={{ marginTop: '0.5rem', fontSize: '0.75rem', color: '#9ca3af' }}>
                      Atualizado em: {lastUpdated}
                    </p>
                  </div>
                  <button 
                    className="btn-download" 
                    style={{ justifyContent: 'center', width: '100%', marginTop: 'auto' }}
                    onClick={() => handleDownload(title)}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                      <polyline points="7 10 12 15 17 10"></polyline>
                      <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                    Download Dataset
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export default ExportacaoDados;
