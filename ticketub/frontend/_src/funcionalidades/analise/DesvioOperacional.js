import React, { useEffect, useState } from 'react';
import { apiGet } from '../../logica_do_sistema/services/apiClient';
import './DesvioOperacional.css';

const IconCompass = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#3b82f6' }}>
    <circle cx="12" cy="12" r="10" />
    <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" />
  </svg>
);

const IconAlert = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#ef4444' }}>
    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </svg>
);

const IconActivity = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#10b981' }}>
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
  </svg>
);

function DesvioOperacional() {
  const [qualidade, setQualidade] = useState(null);
  const [desvios, setDesvios] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function carregarDados() {
      setLoading(true);
      setError(null);
      try {
        const [resQualidade, resDesvios] = await Promise.all([
          apiGet('/api/desvio-operacional/qualidade-localizacao'),
          apiGet('/api/desvio-operacional/analise-desvios')
        ]);
        setQualidade(resQualidade);
        setDesvios(resDesvios);
      } catch (err) {
        console.error('Erro ao carregar desvios operacionais:', err);
        setError('Não foi possível carregar os dados de desvio operacional.');
      } finally {
        setLoading(false);
      }
    }
    carregarDados();
  }, []);

  if (loading) {
    return (
      <div className="od-container" style={{ textAlign: 'center', padding: '40px' }}>
        <div className="loader-ring">A carregar métricas operacionais...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="od-container" style={{ padding: '24px' }}>
        <div style={{ backgroundColor: '#fee2e2', border: '1px solid #fca5a5', color: '#b91c1c', padding: '16px', borderRadius: '8px' }}>
          ⚠️ {error}
        </div>
      </div>
    );
  }

  return (
    <div className="desvio-container">
      <div className="map-header" style={{ marginBottom: '16px' }}>
        <h2>Desvio e Correlação Operacional</h2>
        <p style={{ margin: '4px 0 0 0', color: '#64748b', fontSize: '0.9rem' }}>
          Análise de cruzamento das validações com telemetria GPS (SAE-IP) e desvios de horários da frota TUB.
        </p>
      </div>

      {/* Alertas de Qualidade */}
      {qualidade && qualidade.alertaDegradacaoQualidade && (
        <div className="desvio-alert desvio-alert-warning">
          <IconAlert />
          <div>
            <strong>Alerta de Qualidade de Localização Degradada:</strong> A percentagem de localizações indeterminadas está acima de 10% ({qualidade.percentagemIndeterminada}%). Por favor verifique as ligações dos validadores.
          </div>
        </div>
      )}

      {qualidade && qualidade.alertaSaeIp && (
        <div className="desvio-alert desvio-alert-danger">
          <IconAlert />
          <div>
            <strong>Alerta de Falha do Serviço SAE-IP:</strong> A cobertura de localização exata por GPS está abaixo do limiar de 50% face às estimadas por horário.
          </div>
        </div>
      )}

      {/* Grid Superior: Qualidade de Localização vs KPIs Operacionais */}
      <div className="desvio-grid">
        
        {/* Bloco 1: Qualidade da Localização */}
        <div className="desvio-card">
          <div className="desvio-card-header">
            <h2>
              <IconCompass />
              Qualidade da Localização
            </h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div className="desvio-progress-group">
              <div className="desvio-progress-info">
                <span className="desvio-progress-label">GPS Preciso (Telemetria Real)</span>
                <span className="desvio-progress-value" style={{ color: '#10b981' }}>{qualidade?.percentagemGpsPreciso}%</span>
              </div>
              <div className="desvio-progress-bar">
                <div className="desvio-progress-fill" style={{ width: `${qualidade?.percentagemGpsPreciso}%`, backgroundColor: '#10b981' }}></div>
              </div>
            </div>

            <div className="desvio-progress-group">
              <div className="desvio-progress-info">
                <span className="desvio-progress-label">Estimada por Horário (Schedule)</span>
                <span className="desvio-progress-value" style={{ color: '#3b82f6' }}>{qualidade?.percentagemEstimadaSchedule}%</span>
              </div>
              <div className="desvio-progress-bar">
                <div className="desvio-progress-fill" style={{ width: `${qualidade?.percentagemEstimadaSchedule}%`, backgroundColor: '#3b82f6' }}></div>
              </div>
            </div>

            <div className="desvio-progress-group">
              <div className="desvio-progress-info">
                <span className="desvio-progress-label">Localização Indeterminada</span>
                <span className="desvio-progress-value" style={{ color: '#ef4444' }}>{qualidade?.percentagemIndeterminada}%</span>
              </div>
              <div className="desvio-progress-bar">
                <div className="desvio-progress-fill" style={{ width: `${qualidade?.percentagemIndeterminada}%`, backgroundColor: '#ef4444' }}></div>
              </div>
            </div>
          </div>
        </div>

        {/* Bloco 2: Indicadores Planeado vs Executado */}
        <div className="desvio-card">
          <div className="desvio-card-header">
            <h2>
              <IconActivity />
              Serviço Executado vs. Planeado
            </h2>
          </div>

          <div className="desvio-stats-grid">
            <div className="desvio-stat-box">
              <span className="desvio-stat-label">Rácio Procura/Oferta</span>
              <span className="desvio-stat-value">{desvios?.procuraVsOfertaRatio}</span>
              <span className="desvio-stat-sub success">Dentro da meta TUB</span>
            </div>

            <div className="desvio-stat-box">
              <span className="desvio-stat-label">Taxa Geral Atrasos</span>
              <span className="desvio-stat-value danger">{desvios?.taxaAtrasoLinhapct}%</span>
              <span className="desvio-stat-sub danger">Atraso &gt; 5 min</span>
            </div>

            <div className="desvio-stat-box desvio-stat-box-full">
              <span className="desvio-stat-label">Zona Crítica Impactada</span>
              <span className="desvio-stat-value" style={{ fontSize: '1.05rem', fontWeight: '700' }}>{desvios?.impactoGeoAtraso}</span>
            </div>
          </div>
        </div>

      </div>

      {/* Tabela de Desvios de Viagens em Tempo Real */}
      <div className="desvio-card">
        <div className="desvio-card-header">
          <h2>Monitorização de Viagens e Desvios em Tempo Real</h2>
        </div>
        <div className="desvio-table-container">
          <table className="desvio-table">
            <thead>
              <tr>
                <th>Linha / Rota</th>
                <th>Atraso Estimado</th>
                <th>Velocidade (Real vs Planeada)</th>
                <th>Estado Operacional</th>
              </tr>
            </thead>
            <tbody>
              {desvios?.desviosDetalhados.map((item, idx) => (
                <tr key={idx}>
                  <td style={{ fontWeight: '600', color: '#0f172a' }}>Linha {item.routeId}</td>
                  <td style={{ color: '#1e293b' }}>
                    {item.atrasoMinutos === 0 ? 'Sem Atraso' : `+${item.atrasoMinutos} min`}
                  </td>
                  <td style={{ color: '#475569', fontSize: '0.9rem' }}>
                    {item.velocidadeExecutadaKmh} km/h vs {item.velocidadeEstimadaKmh} km/h
                  </td>
                  <td>
                    {item.naoApresentacao ? (
                      <span className="desvio-badge desvio-badge-nao-apresentacao">
                        NÃO-APRESENTAÇÃO
                      </span>
                    ) : item.status === 'ATRASADO_SIGNIFICATIVO' ? (
                      <span className="desvio-badge desvio-badge-atraso-significativo">
                        ATRASO SIGNIFICATIVO
                      </span>
                    ) : item.status === 'ATRASADO_LIGEIRO' ? (
                      <span className="desvio-badge desvio-badge-atraso-ligeiro">
                        ATRASO LIGEIRO
                      </span>
                    ) : (
                      <span className="desvio-badge desvio-badge-limiar">
                        DENTRO DO LIMIAR
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default DesvioOperacional;
