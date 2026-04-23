import React, { useState, useCallback } from 'react';
import EcraGestaoMapeamentos from '../management/EcraGestaoMapeamentos';
import EcraRevisaoEventos from '../audit/EcraRevisaoEventos';
import CategorizationStats from './CategorizationStats';
import './ModuloBilhetes.css';

const ModuloBilhetes = () => {
  const [activeSubTab, setActiveSubTab] = useState(() => {
    return localStorage.getItem('bilhetes_active_tab') || 'mapeamentos';
  });

  // Estado centralizado para forçar o refresh das estatísticas
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleTabChange = (tabName) => {
    setActiveSubTab(tabName);
    localStorage.setItem('bilhetes_active_tab', tabName);
  };

  const handleMappingChange = useCallback(() => {
    setRefreshTrigger(prev => prev + 1);
  }, []);

  return (
    <div className="modulo-bilhetes-container">
      <div className="bilhetes-nav">
        <button 
          className={`sub-nav-btn ${activeSubTab === 'mapeamentos' ? 'active' : ''}`}
          onClick={() => handleTabChange('mapeamentos')}
        >
          Gestão de Mapeamentos
        </button>
        <button 
          className={`sub-nav-btn ${activeSubTab === 'revisao' ? 'active' : ''}`}
          onClick={() => handleTabChange('revisao')}
        >
          Revisão de Eventos (Não Categorizados)
        </button>
      </div>

      <div className="bilhetes-content">
        {/* As estatísticas agora são geridas aqui para serem reativas a ambos os ecrãs */}
        <CategorizationStats refreshTrigger={refreshTrigger} />
        
        {activeSubTab === 'mapeamentos' && (
          <EcraGestaoMapeamentos onMappingChange={handleMappingChange} />
        )}
        {activeSubTab === 'revisao' && (
          <EcraRevisaoEventos onMappingChange={handleMappingChange} />
        )}
      </div>
    </div>
  );
};

export default ModuloBilhetes;


