import React, { useState, useEffect, useCallback } from 'react';
import { 
  getMetricasIngestao, 
  getComparacaoPeriodos, 
  getSeriesTemporais, 
  exportarHistorico, 
  reprocessarPeriodo 
} from '../../logica_do_sistema/services/analiseService';
import CustomDatePicker from './CustomDatePicker';
import './Analise.css';

const LABELS = {
  volumeIngestao:  'Volume Total',
  validas:         'Válidas',
  quarentena:      'Quarentena',
  duplicados:      'Duplicados',
  taxaValidas:     'Taxa Válidas (%)',
  taxaQuarentena:  'Taxa Quarentena (%)',
  estado:          'Estado',
};

const label = (key) => LABELS[key] || key;

const renderDesvio = (num) => {
  if (isNaN(num)) return null;
  if (num > 0)  return <span className="desvio-positivo" style={{ marginLeft: '10px' }}>▲ +{num}%</span>;
  if (num < 0) return <span className="desvio-negativo" style={{ marginLeft: '10px' }}>▼ {num}%</span>;
  return <span className="desvio-neutro" style={{ marginLeft: '10px' }}>{num}%</span>;
};

const renderEstadoBadge = (metric) => {
  if (!metric) return '—';
  if (metric.taxaValidas > 80) {
    return <span className="historico-status-badge status-good">BOM</span>;
  }
  if (metric.taxaValidas >= 60) {
    return <span className="historico-status-badge status-attention">ATENÇÃO</span>;
  }
  return <span className="historico-status-badge status-critical">CRÍTICO</span>;
};

function HistoricoConsolidado() {
  // Tabs: 'comparacao', 'series', 'export', 'recovery'
  const [activeTab, setActiveTab] = useState('comparacao');

  const getDefaultDates = () => {
    const fim = new Date();
    const inicio = new Date();
    inicio.setDate(fim.getDate() - 30);
    return {
      inicio: inicio.toISOString().split('T')[0],
      fim:    fim.toISOString().split('T')[0],
      compInicio: '',
      compFim: ''
    };
  };

  const [dates, setDates] = useState(getDefaultDates());
  const [metricas, setMetricas] = useState(null);
  const [comparacao, setComparacao] = useState(null);
  const [seriesData, setSeriesData] = useState([]);
  const [exportResult, setExportResult] = useState(null);
  
  // Filters for Series Temporais
  const [seriesFilters, setSeriesFilters] = useState({
    routeId: '',
    perfilTarifario: '',
    granularidade: 'DIA'
  });

  // Recovery partition state
  const [recoveryDates, setRecoveryDates] = useState({
    inicio: new Date().toISOString().split('T')[0],
    fim: new Date().toISOString().split('T')[0]
  });
  const [recoveryMessage, setRecoveryMessage] = useState(null);
  const [recoveryLoading, setRecoveryLoading] = useState(false);

  const [loading, setLoading] = useState(true);
  const [exportFormat, setExportFormat] = useState('CSV');
  const [exportUser, setExportUser] = useState('analista');

  // Load Comparative Period & global metrics
  const fetchComparative = useCallback(async () => {
    setLoading(true);
    try {
      const [metricasData, comparacaoData] = await Promise.all([
        getMetricasIngestao(),
        getComparacaoPeriodos(dates.inicio, dates.fim, dates.compInicio, dates.compFim),
      ]);
      setMetricas(metricasData);
      setComparacao(comparacaoData);
    } catch (err) {
      console.error(err);
      setMetricas(null);
      setComparacao(null);
    } finally {
      setLoading(false);
    }
  }, [dates]);

  // Load Series Temporais data
  const fetchSeries = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getSeriesTemporais({
        ...seriesFilters,
        inicio: dates.inicio,
        fim: dates.fim
      });
      setSeriesData(data || []);
    } catch (err) {
      console.error(err);
      setSeriesData([]);
    } finally {
      setLoading(false);
    }
  }, [dates, seriesFilters]);

  useEffect(() => {
    if (activeTab === 'comparacao') {
      fetchComparative();
    } else if (activeTab === 'series') {
      fetchSeries();
    }
  }, [activeTab, fetchComparative, fetchSeries]);

  const handleExport = async () => {
    setLoading(true);
    try {
      // Simulate DPO header permission
      const result = await exportarHistorico({
        inicio: dates.inicio,
        fim: dates.fim,
        formato: exportFormat,
        routeId: seriesFilters.routeId,
        user: exportUser // Will map to X-Api-User header mapping
      });
      setExportResult(result);
    } catch (err) {
      alert("Falha ao exportar: utilizador sem permissões ou falha no backend.");
    } finally {
      setLoading(false);
    }
  };

  const handleRecovery = async () => {
    setRecoveryLoading(true);
    setRecoveryMessage(null);
    try {
      const res = await reprocessarPeriodo(recoveryDates.inicio, recoveryDates.fim);
      if (res.status === 'SUCESSO') {
        setRecoveryMessage({ type: 'success', text: res.mensagem });
      } else {
        setRecoveryMessage({ type: 'error', text: res.erro || 'Falha desconhecida.' });
      }
    } catch (err) {
      setRecoveryMessage({ type: 'error', text: 'Erro de ligação ao backend.' });
    } finally {
      setRecoveryLoading(false);
    }
  };

  // Find max value in series for bar scaling
  const maxValidacoes = seriesData.reduce((max, item) => item.totalValidacoes > max ? item.totalValidacoes : max, 1);

  return (
    <div className="analise-container">
      <div className="analise-card">
        <div className="analise-card-header">
          <h2>
            Histórico Consolidado & Comparação
          </h2>
        </div>

        {/* Tab Navigation */}
        <div className="analise-tabs" style={{ display: 'flex', gap: '1rem', borderBottom: '2px solid #e2e8f0', marginBottom: '1.25rem' }}>
          <button 
            className={`analise-tab ${activeTab === 'comparacao' ? 'active' : ''}`}
            onClick={() => setActiveTab('comparacao')}
          >
            Comparação de Períodos
          </button>
          <button 
            className={`analise-tab ${activeTab === 'series' ? 'active' : ''}`}
            onClick={() => setActiveTab('series')}
          >
            Séries Temporais
          </button>
          <button 
            className={`analise-tab ${activeTab === 'export' ? 'active' : ''}`}
            onClick={() => setActiveTab('export')}
          >
            Exportar Relatório
          </button>
          <button 
            className={`analise-tab ${activeTab === 'recovery' ? 'active' : ''}`}
            onClick={() => setActiveTab('recovery')}
          >
            Recuperação Incremental (Admin)
          </button>
        </div>

        {/* Filters Section (Common to Comparacao and Series) */}
        {(activeTab === 'comparacao' || activeTab === 'series') && (
          <div className="historico-filters" style={{ display: 'flex', gap: '1.5rem', padding: '1.25rem', backgroundColor: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0', boxSizing: 'border-box', alignItems: 'flex-end', width: '100%' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', flex: 1 }}>
              <CustomDatePicker
                label="Período Principal - Início"
                value={dates.inicio}
                onChange={(val) => setDates(prev => ({ ...prev, inicio: val }))}
              />
              <CustomDatePicker
                label="Fim"
                value={dates.fim}
                onChange={(val) => setDates(prev => ({ ...prev, fim: val }))}
              />
            </div>

            {activeTab === 'comparacao' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', flex: 1, borderLeft: '2px solid #e2e8f0', paddingLeft: '1.5rem' }}>
                <CustomDatePicker
                  label="Comparar com - Início"
                  value={dates.compInicio}
                  onChange={(val) => setDates(prev => ({ ...prev, compInicio: val }))}
                />
                <CustomDatePicker
                  label="Fim"
                  value={dates.compFim}
                  onChange={(val) => setDates(prev => ({ ...prev, compFim: val }))}
                />
              </div>
            )}

            {activeTab === 'series' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', flex: 1.5, borderLeft: '2px solid #e2e8f0', paddingLeft: '1.5rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', width: '100%' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Linha</label>
                  <select 
                    style={{ padding: '0.45rem 0.85rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#ffffff', width: '100%', height: '38px', fontSize: '0.9rem', color: '#0f172a', fontWeight: '500' }}
                    value={seriesFilters.routeId}
                    onChange={(e) => setSeriesFilters(prev => ({ ...prev, routeId: e.target.value }))}
                  >
                    <option value="">Todas</option>
                    <option value="13">Linha 13</option>
                    <option value="42">Linha 42</option>
                    <option value="95">Linha 95</option>
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', width: '100%' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Perfil</label>
                  <select 
                    style={{ padding: '0.45rem 0.85rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#ffffff', width: '100%', height: '38px', fontSize: '0.9rem', color: '#0f172a', fontWeight: '500' }}
                    value={seriesFilters.perfilTarifario}
                    onChange={(e) => setSeriesFilters(prev => ({ ...prev, perfilTarifario: e.target.value }))}
                  >
                    <option value="">Todos</option>
                    <option value="NORMAL">Normal</option>
                    <option value="ESTUDANTE">Estudante</option>
                    <option value="SENIOR">Sénior</option>
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', width: '100%' }}>
                  <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Granularidade</label>
                  <select 
                    style={{ padding: '0.45rem 0.85rem', border: '1px solid #cbd5e1', borderRadius: '8px', backgroundColor: '#ffffff', width: '100%', height: '38px', fontSize: '0.9rem', color: '#0f172a', fontWeight: '500' }}
                    value={seriesFilters.granularidade}
                    onChange={(e) => setSeriesFilters(prev => ({ ...prev, granularidade: e.target.value }))}
                  >
                    <option value="DIA">Diária</option>
                    <option value="HORA_08">Hora (08:00 - 09:00)</option>
                    <option value="HORA_18">Hora (18:00 - 19:00)</option>
                  </select>
                </div>
              </div>
            )}

            <button 
              onClick={activeTab === 'comparacao' ? fetchComparative : fetchSeries}
              style={{ height: '38px', padding: '0 1.5rem', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '8px', cursor: 'pointer', fontWeight: 700, fontSize: '0.9rem', flexShrink: 0, transition: 'background-color 0.2s' }}
            >
              Atualizar
            </button>
          </div>
        )}

        {/* Tab 1: Comparação de Períodos */}
        {activeTab === 'comparacao' && (
          <div className="analise-card-body">
            {loading ? (
              <div className="analise-empty-state">A calcular estatísticas comparativas...</div>
            ) : (
              <>
                {/* Metadados Comparação */}
                {comparacao && (
                  <div style={{ background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '12px', padding: '1.25rem', marginBottom: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <span style={{ fontSize: '0.9rem', color: '#1e3a8a', fontWeight: 'bold', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                      Resumo da Análise Comparativa
                    </span>
                    <h3 style={{ margin: 0, color: '#1e40af', fontSize: '1.2rem', fontWeight: 800 }}>
                      {comparacao.resumoTextual}
                    </h3>
                  </div>
                )}

                {/* Box Grid */}
                {comparacao && (
                  <div className="historico-metrics-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '2rem' }}>
                    
                    {/* Validações Comparação */}
                    <div className="historico-metric-box" style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '1.5rem' }}>
                      <span className="historico-metric-label">Validações Totais</span>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem' }}>
                        <div>
                          <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>Período Selecionado:</p>
                          <span style={{ fontSize: '1.8rem', fontWeight: 800, color: '#1e293b' }}>
                            {comparacao.periodoAtual?.validacoes}
                          </span>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>Período Anterior:</p>
                          <span style={{ fontSize: '1.3rem', fontWeight: 600, color: '#475569' }}>
                            {comparacao.periodoComparacao?.validacoes}
                          </span>
                        </div>
                      </div>
                      <div style={{ marginTop: '1rem', borderTop: '1px solid #f1f5f9', paddingTop: '0.8rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <span style={{ fontSize: '0.85rem', color: '#475569', fontWeight: 600 }}>Diferença Percentual:</span>
                        {renderDesvio(comparacao.variacaoValidacoes)}
                      </div>
                    </div>

                    {/* Receita Comparação */}
                    <div className="historico-metric-box" style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '1.5rem' }}>
                      <span className="historico-metric-label">Receita Realizada (Est.)</span>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem' }}>
                        <div>
                          <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>Período Selecionado:</p>
                          <span style={{ fontSize: '1.8rem', fontWeight: 800, color: '#16a34a' }}>
                            {parseFloat(comparacao.periodoAtual?.receita || 0).toFixed(2)} €
                          </span>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>Período Anterior:</p>
                          <span style={{ fontSize: '1.3rem', fontWeight: 600, color: '#475569' }}>
                            {parseFloat(comparacao.periodoComparacao?.receita || 0).toFixed(2)} €
                          </span>
                        </div>
                      </div>
                      <div style={{ marginTop: '1rem', borderTop: '1px solid #f1f5f9', paddingTop: '0.8rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <span style={{ fontSize: '0.85rem', color: '#475569', fontWeight: 600 }}>Diferença Percentual:</span>
                        {renderDesvio(comparacao.variacaoReceita)}
                      </div>
                    </div>
                  </div>
                )}

                {/* Métricas Globais Adicionais */}
                {metricas && (
                  <div style={{ marginTop: '2rem' }}>
                    <h3 className="historico-section-title">Métricas de Ingestão do Data Lake</h3>
                    {['ultimaHora', 'ultimas24Horas', 'ultimos7Dias']
                      .filter(k => k in metricas)
                      .map(k => (
                        <div key={k} className="historico-periodo-section" style={{ background: '#ffffff', border: '1px solid #e2e8f0', padding: '1.25rem', borderRadius: '10px', marginBottom: '1rem' }}>
                          <h4 className="historico-periodo-title" style={{ color: '#3b82f6', marginBottom: '0.8rem' }}>
                            {k === 'ultimaHora' ? 'Última Hora' : k === 'ultimas24Horas' ? 'Últimas 24 Horas' : 'Últimos 7 Dias'}
                          </h4>
                          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '1rem' }}>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>VOLUME TOTAL</span>
                              <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>{metricas[k].volumeIngestao}</span>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>VÁLIDAS</span>
                              <span style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#16a34a' }}>
                                {metricas[k].validas} <span style={{ fontSize: '0.8rem', color: '#64748b' }}>({metricas[k].taxaValidas}%)</span>
                              </span>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>QUARENTENA</span>
                              <span style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#ca8a04' }}>
                                {metricas[k].quarentena} <span style={{ fontSize: '0.8rem', color: '#64748b' }}>({metricas[k].taxaQuarentena}%)</span>
                              </span>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>DUPLICADOS</span>
                              <span style={{ fontSize: '1.2rem', fontWeight: 'bold', color: '#ef4444' }}>{metricas[k].duplicados}</span>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column' }}>
                              <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 'bold' }}>ESTADO</span>
                              <div>{renderEstadoBadge(metricas[k].estado)}</div>
                            </div>
                          </div>
                        </div>
                      ))}
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Tab 2: Séries Temporais */}
        {activeTab === 'series' && (
          <div className="analise-card-body">
            {loading ? (
              <div className="analise-empty-state">A carregar séries temporais...</div>
            ) : (
              <>
                {/* Premium CSS Bar Chart */}
                {seriesData.length > 0 ? (
                  <div style={{ background: '#ffffff', padding: '1.5rem', borderRadius: '12px', border: '1px solid #e2e8f0', marginBottom: '2rem' }}>
                    <h4 style={{ margin: '0 0 1.25rem 0', color: '#1e293b' }}>Evolução de Validações no Período</h4>
                    <div style={{ display: 'flex', alignItems: 'flex-end', height: '160px', gap: '6px', borderBottom: '2px solid #e2e8f0', paddingBottom: '8px' }}>
                      {seriesData.map((item, idx) => {
                        const heightPercent = (item.totalValidacoes / maxValidacoes) * 100;
                        return (
                          <div key={idx} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%', justifyContent: 'flex-end' }}>
                            <span style={{ fontSize: '0.75rem', fontWeight: 'bold', color: '#64748b', marginBottom: '4px' }}>
                              {item.totalValidacoes}
                            </span>
                            <div 
                              style={{ 
                                width: '100%', 
                                height: `${Math.max(heightPercent, 5)}%`, 
                                background: 'linear-gradient(to top, #3b82f6, #60a5fa)', 
                                borderRadius: '4px 4px 0 0',
                                transition: 'height 0.3s ease'
                              }} 
                              title={`Data: ${item.periodoInicio} - Validações: ${item.totalValidacoes}`}
                            />
                            <span style={{ fontSize: '0.7rem', color: '#64748b', marginTop: '6px', textTransform: 'uppercase', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100%' }}>
                              {item.periodoInicio.split('-')[2]}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ) : (
                  <div className="analise-empty-state" style={{ marginBottom: '2rem' }}>
                    Nenhum dado consolidado encontrado no período e filtros selecionados.
                  </div>
                )}

                {/* Data Grid Table */}
                <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', overflow: 'hidden' }}>
                  <table className="analise-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead>
                      <tr style={{ backgroundColor: '#f8fafc' }}>
                        <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>Período</th>
                        <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>Linha / Route</th>
                        <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>Perfil Tarifário</th>
                        <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #cbd5e1' }}>Granularidade</th>
                        <th style={{ padding: '12px', textAlign: 'right', borderBottom: '1px solid #cbd5e1' }}>Validações Totais</th>
                        <th style={{ padding: '12px', textAlign: 'right', borderBottom: '1px solid #cbd5e1' }}>Receita Estimada</th>
                      </tr>
                    </thead>
                    <tbody>
                      {seriesData.length > 0 ? (
                        seriesData.map((row, idx) => (
                          <tr key={idx} style={{ borderBottom: '1px solid #f1f5f9' }}>
                            <td style={{ padding: '12px' }}>{row.periodoInicio}</td>
                            <td style={{ padding: '12px' }}>{row.routeId || 'Geral (Todos)'}</td>
                            <td style={{ padding: '12px' }}>{row.perfilTarifario || 'Geral (Todos)'}</td>
                            <td style={{ padding: '12px', textTransform: 'capitalize' }}>{row.granularidade}</td>
                            <td style={{ padding: '12px', textAlign: 'right', fontWeight: 'bold' }}>{row.totalValidacoes}</td>
                            <td style={{ padding: '12px', textAlign: 'right', color: '#16a34a', fontWeight: 'bold' }}>
                              {parseFloat(row.receitaEstimada || 0).toFixed(2)} €
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="6" style={{ padding: '24px', textAlign: 'center', color: '#94a3b8' }}>
                            Nenhum registo consolidado.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        )}

        {/* Tab 3: Exportação & Auditoria */}
        {activeTab === 'export' && (
          <div className="analise-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ background: '#f8fafc', padding: '1.5rem', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
              <h3 style={{ margin: '0 0 1rem 0', color: '#0f172a' }}>Exportação de Snapshot Histórico (DPO / Conformidade)</h3>
              
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.25rem', marginBottom: '1.5rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                    Formato de Exportação:
                  </label>
                  <select 
                    style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                    value={exportFormat}
                    onChange={(e) => setExportFormat(e.target.value)}
                  >
                    <option value="CSV">Ficheiro CSV (.csv)</option>
                    <option value="EXCEL">Excel (.xlsx) com gráficos</option>
                    <option value="PDF">Relatório Formato PDF (.pdf)</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: '#475569', marginBottom: '6px' }}>
                    Simular Utilizador (Registo Auditoria):
                  </label>
                  <select 
                    style={{ width: '100%', padding: '0.5rem 0.8rem', border: '1px solid #cbd5e1', borderRadius: '8px' }}
                    value={exportUser}
                    onChange={(e) => setExportUser(e.target.value)}
                  >
                    <option value="analista">Direção / Analista Operacional</option>
                    <option value="admin">Administrador IT</option>
                    <option value="dpo">Data Protection Officer (DPO)</option>
                    <option value="gestor">Gestor Financeiro</option>
                    <option value="operador_irregular">Outro (Sem Permissões)</option>
                  </select>
                </div>

                <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <button 
                    onClick={handleExport}
                    style={{ 
                      width: '100%', 
                      padding: '0.55rem', 
                      background: '#16a34a', 
                      color: '#ffffff', 
                      border: 'none', 
                      borderRadius: '8px', 
                      fontWeight: 'bold', 
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '8px'
                    }}
                  >
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                    Exportar e Auditar
                  </button>
                </div>
              </div>
            </div>

            {/* Resultado da Exportação */}
            {exportResult && (
              <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '1.5rem', animation: 'fadeIn 0.2s' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f1f5f9', paddingBottom: '0.8rem', marginBottom: '1rem' }}>
                  <h4 style={{ margin: 0, color: '#16a34a' }}>Exportação Concluída com Sucesso</h4>
                  <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                    ID de Auditoria: {exportResult.exportTime ? 'Auditado às ' + exportResult.exportTime.split('T')[1].substring(0, 8) : 'N/A'}
                  </span>
                </div>

                <p style={{ fontSize: '0.9rem', color: '#475569' }}>
                  O sistema gerou o snapshot de dados e registou a ação em conformidade na base de dados de auditoria (DPO). 
                  O ficheiro em formato <strong>{exportResult.formato}</strong> contém <strong>{exportResult.totalRegistos} registos</strong> para o período de <strong>{exportResult.periodo}</strong>.
                </p>

                <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0', maxHeight: '180px', overflowY: 'auto' }}>
                  <pre style={{ margin: 0, fontSize: '0.75rem', fontFamily: 'monospace' }}>
                    {JSON.stringify(exportResult.dados, null, 2)}
                  </pre>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab 4: Recuperação Incremental (Admin IT) */}
        {activeTab === 'recovery' && (
          <div className="analise-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ background: '#fef2f2', border: '1px solid #fee2e2', borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '1rem' }}>
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#dc2626" strokeWidth="2">
                  <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <h3 style={{ margin: 0, color: '#991b1b' }}>Fila de Recuperação Incremental & Reprocessamento (FA1)</h3>
              </div>

              <p style={{ fontSize: '0.9rem', color: '#7f1d1d', marginBottom: '1.25rem' }}>
                Utilize esta consola técnica para forçar o reprocessamento parcial de séries temporais se detetar períodos sem dados ou falhas na ingestão. O sistema recalculará incrementalmente a partição indicada sem duplicar os registos.
              </p>

              <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'flex-end', backgroundColor: '#ffffff', padding: '1.25rem', borderRadius: '12px', border: '1px solid #fecaca', boxSizing: 'border-box', width: '100%' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', flex: 1 }}>
                  <CustomDatePicker
                    label="Início do Intervalo"
                    value={recoveryDates.inicio}
                    onChange={(val) => setRecoveryDates(prev => ({ ...prev, inicio: val }))}
                  />
                  <CustomDatePicker
                    label="Fim do Intervalo"
                    value={recoveryDates.fim}
                    onChange={(val) => setRecoveryDates(prev => ({ ...prev, fim: val }))}
                  />
                </div>

                <button 
                  onClick={handleRecovery}
                  disabled={recoveryLoading}
                  style={{ 
                    height: '38px',
                    padding: '0 1.5rem', 
                    background: '#dc2626', 
                    color: '#ffffff', 
                    border: 'none', 
                    borderRadius: '8px', 
                    fontWeight: 'bold', 
                    cursor: recoveryLoading ? 'not-allowed' : 'pointer',
                    opacity: recoveryLoading ? 0.7 : 1,
                    flexShrink: 0
                  }}
                >
                  {recoveryLoading ? 'A Processar...' : 'Reprocessar Intervalo'}
                </button>
              </div>
            </div>

            {/* Mensagem de Feedback */}
            {recoveryMessage && (
              <div style={{ 
                padding: '1rem 1.25rem', 
                borderRadius: '8px', 
                border: '1px solid', 
                backgroundColor: recoveryMessage.type === 'success' ? '#dcfce7' : '#fee2e2', 
                borderColor: recoveryMessage.type === 'success' ? '#bbf7d0' : '#fecaca', 
                color: recoveryMessage.type === 'success' ? '#15803d' : '#b91c1c'
              }}>
                <strong>{recoveryMessage.type === 'success' ? 'Sucesso: ' : 'Erro na Recuperação: '}</strong>
                {recoveryMessage.text}
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
}

export default HistoricoConsolidado;